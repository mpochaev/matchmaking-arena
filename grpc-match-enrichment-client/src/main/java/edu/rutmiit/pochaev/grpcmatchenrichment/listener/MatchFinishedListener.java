package edu.rutmiit.pochaev.grpcmatchenrichment.listener;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import edu.rutmiit.pochaev.grpcmatchenrichment.publisher.MatchEnrichmentEventPublisher;
import edu.rutmiit.pochaev.matchmakingevents.EventMetadata;
import edu.rutmiit.pochaev.matchmakingevents.MatchEvent;
import edu.rutmiit.pochaev.matchmakinggrpc.CalculateRankChangesRequest;
import edu.rutmiit.pochaev.matchmakinggrpc.MatchPlayer;
import edu.rutmiit.pochaev.matchmakinggrpc.MatchRankEnrichmentGrpc;
import edu.rutmiit.pochaev.matchmakinggrpc.RankChangesResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Передает событие из RabbitMQ в gRPC и обратно.
 * Получает match.finished, вызывает gRPC-сервер расчета рейтинга и публикует match.enriched.
 */
@Component
public class MatchFinishedListener {

    private static final Logger log = LoggerFactory.getLogger(MatchFinishedListener.class);

    private final MatchRankEnrichmentGrpc.MatchRankEnrichmentBlockingStub rankStub;
    private final MatchEnrichmentEventPublisher enrichmentPublisher;
    private final JsonMapper jsonMapper;

    public MatchFinishedListener(MatchRankEnrichmentGrpc.MatchRankEnrichmentBlockingStub rankStub,
                                 MatchEnrichmentEventPublisher enrichmentPublisher,
                                 JsonMapper jsonMapper) {
        this.rankStub = rankStub;
        this.enrichmentPublisher = enrichmentPublisher;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = "q.matchmaking.enrichment.match-finished", messageConverter = "")
    @SuppressWarnings("UseSpecificCatch")
    public void handleMatchFinished(Message message) {
        try {
            JsonNode root = jsonMapper.readTree(message.getBody());
            EventMetadata metadata = jsonMapper.treeToValue(root.get("metadata"), EventMetadata.class);
            MatchEvent.Finished matchFinished = jsonMapper.treeToValue(root.get("payload"), MatchEvent.Finished.class);

            log.info("Получено событие match.finished: matchId={}, победитель={} [eventId={}]",
                    matchFinished.matchId(), matchFinished.winnerNickname(), metadata.eventId());

            CalculateRankChangesRequest.Builder requestBuilder = CalculateRankChangesRequest.newBuilder()
                    .setMatchId(matchFinished.matchId().toString())
                    .setMode(matchFinished.mode() != null ? matchFinished.mode() : "")
                    .setRegion(matchFinished.region() != null ? matchFinished.region() : "")
                    .setWinnerPlayerId(matchFinished.winnerPlayerId() == null ? "" : matchFinished.winnerPlayerId().toString())
                    .setWinnerNickname(matchFinished.winnerNickname() != null ? matchFinished.winnerNickname() : "");

            for (MatchEvent.MatchPlayer player : matchFinished.players()) {
                requestBuilder.addPlayers(MatchPlayer.newBuilder()
                        .setPlayerId(player.playerId().toString())
                        .setNickname(player.nickname())
                        .setRating(player.rating())
                        .setRank(player.rank())
                        .setWinner(player.playerId().equals(matchFinished.winnerPlayerId()))
                        .build());
            }

            log.info("Вызов gRPC: MatchRankEnrichment.CalculateRankChanges(matchId={})", matchFinished.matchId());
            RankChangesResponse grpcResponse = rankStub.calculateRankChanges(requestBuilder.build());

            log.info("Ответ gRPC получен: matchId={}, {}", grpcResponse.getMatchId(), grpcResponse.getSummary());

            MatchEvent.Enriched enrichedEvent = new MatchEvent.Enriched(
                    UUID.fromString(grpcResponse.getMatchId()),
                    grpcResponse.getWinnerNickname(),
                    grpcResponse.getChangesList().stream()
                            .map(change -> new MatchEvent.PlayerRankChange(
                                    UUID.fromString(change.getPlayerId()),
                                    change.getNickname(),
                                    change.getOldRating(),
                                    change.getDelta(),
                                    change.getNewRating(),
                                    change.getOldRank(),
                                    change.getNewRank()
                            ))
                            .toList(),
                    grpcResponse.getSummary()
            );

            enrichmentPublisher.publishEnriched(enrichedEvent);
            log.info("Изменения рейтинга рассчитаны: matchId={}, событие match.enriched отправлено", matchFinished.matchId());

        } catch (io.grpc.StatusRuntimeException e) {
            log.error("Ошибка gRPC при обогащении матча: {} ({})",
                    e.getStatus().getDescription(), e.getStatus().getCode());
            throw new RuntimeException("Вызов gRPC завершился ошибкой", e);
        } catch (Exception e) {
            log.error("Ошибка обработки match.finished: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать match.finished", e);
        }
    }
}
