package edu.rutmiit.demo.matchlogservice.listener;

import edu.rutmiit.demo.matchlogservice.model.MatchLogEntry;
import edu.rutmiit.demo.matchlogservice.storage.MatchLogStorage;
import edu.rutmiit.demo.matchmakingevents.EventMetadata;
import edu.rutmiit.demo.matchmakingevents.LobbyEvent;
import edu.rutmiit.demo.matchmakingevents.MatchEvent;
import edu.rutmiit.demo.matchmakingevents.PlayerEvent;
import edu.rutmiit.demo.matchmakingevents.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * Сервис принимает сырое JSON-сообщение и читает metadata.eventType
 * По нему payload превращается в нужный record.
 */
@Component
public class MatchLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(MatchLogEventListener.class);

    private final MatchLogStorage matchLogStorage;
    private final JsonMapper jsonMapper;

    public MatchLogEventListener(MatchLogStorage matchLogStorage, JsonMapper jsonMapper) {
        this.matchLogStorage = matchLogStorage;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = "q.matchmaking.match-log.events", messageConverter = "")
    public void handleEvent(Message message) {
        try {
            JsonNode root = jsonMapper.readTree(message.getBody());

            EventMetadata metadata = jsonMapper.treeToValue(root.get("metadata"), EventMetadata.class);

            if (matchLogStorage.isDuplicate(metadata.eventId())) {
                log.warn("Повторное событие пропущено: eventId={}", metadata.eventId());
                return;
            }

            JsonNode payloadNode = root.get("payload");
            String description = buildDescription(metadata.eventType(), payloadNode);

            MatchLogEntry entry = matchLogStorage.save(new MatchLogEntry(
                    0,
                    metadata.eventId(),
                    metadata.eventType(),
                    metadata.source(),
                    metadata.timestamp(),
                    Instant.now(),
                    description
            ));

            log.info("[MATCH-LOG #{}] {} | {}", entry.sequenceNumber(), metadata.eventType(), description);
        } catch (Exception e) {
            log.error("Ошибка обработки события: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать событие", e);
        }
    }

    private String buildDescription(String eventType, JsonNode payloadNode) throws Exception {
        return switch (eventType) {
            case RoutingKeys.PLAYER_CREATED -> {
                PlayerEvent.Created e = jsonMapper.treeToValue(payloadNode, PlayerEvent.Created.class);
                yield String.format("Игрок создан: %s (рейтинг=%d, регион=%s, ранг=%s)",
                        e.nickname(), e.rating(), e.region(), e.rank());
            }
            case RoutingKeys.LOBBY_CREATED -> {
                LobbyEvent.Created e = jsonMapper.treeToValue(payloadNode, LobbyEvent.Created.class);
                yield String.format("Лобби создано: %s/%s/%s, минимум игроков: %d",
                        e.mode(), e.region(), e.rank(), e.minPlayers());
            }
            case RoutingKeys.LOBBY_PLAYER_JOINED -> {
                LobbyEvent.PlayerJoined e = jsonMapper.treeToValue(payloadNode, LobbyEvent.PlayerJoined.class);
                yield String.format("Игрок %s вошел в лобби %s (%d/%d)",
                        e.nickname(), e.lobbyId(), e.playerCount(), e.minPlayers());
            }
            case RoutingKeys.LOBBY_FORMED -> {
                LobbyEvent.Formed e = jsonMapper.treeToValue(payloadNode, LobbyEvent.Formed.class);
                yield String.format("Лобби сформировано: lobby %s, match %s, игроков: %d (%s)",
                        e.lobbyId(), e.matchId(), e.playerCount(), e.playersSummary());
            }
            case RoutingKeys.LOBBY_DISBANDED -> {
                LobbyEvent.Disbanded e = jsonMapper.treeToValue(payloadNode, LobbyEvent.Disbanded.class);
                yield String.format("Лобби %s распущено, причина: %s, игроков: %d",
                        e.lobbyId(), e.reason(), e.playerCount());
            }
            case RoutingKeys.MATCH_PROGRESS -> {
                MatchEvent.Progress e = jsonMapper.treeToValue(payloadNode, MatchEvent.Progress.class);
                yield String.format("Событие матча #%d для матча %s: [%s] %s",
                        e.eventNumber(), e.matchId(), e.eventType(), e.message());
            }
            case RoutingKeys.MATCH_FINISHED -> {
                MatchEvent.Finished e = jsonMapper.treeToValue(payloadNode, MatchEvent.Finished.class);
                yield String.format("Матч %s завершен. Лобби: %s. Игроков: %d. Победитель: %s. %s",
                        e.matchId(), e.lobbyId(), e.playerCount(), e.winnerNickname(), e.resultSummary());
            }
            case RoutingKeys.MATCH_ENRICHED -> {
                MatchEvent.Enriched e = jsonMapper.treeToValue(payloadNode, MatchEvent.Enriched.class);
                String changes = e.changes().stream()
                        .map(change -> String.format("%s: %d до %d (%+d), %s до %s",
                                change.nickname(), change.oldRating(), change.newRating(), change.delta(),
                                change.oldRank(), change.newRank()))
                        .reduce((left, right) -> left + "; " + right)
                        .orElse("изменений нет");
                yield String.format("gRPC-обогащение матча %s: победитель %s. %s. Детали: %s",
                        e.matchId(), e.winnerNickname(), e.summary(), changes);
            }
            default -> "Неизвестное событие: " + eventType;
        };
    }
}
