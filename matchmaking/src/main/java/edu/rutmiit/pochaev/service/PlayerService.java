package edu.rutmiit.pochaev.service;

import edu.rutmiit.pochaev.config.RabbitMQConfig;
import edu.rutmiit.pochaev.event.PlayerEventPublisher;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.InvalidLobbyOperationException;
import edu.rutmiit.pochaev.matchmakingapicontract.exception.ResourceNotFoundException;
import edu.rutmiit.pochaev.matchmakingevents.EventMetadata;
import edu.rutmiit.pochaev.matchmakingevents.MatchEvent;
import edu.rutmiit.pochaev.model.PlayerState;
import edu.rutmiit.pochaev.storage.MatchmakingStorage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.UUID;

@Service
public class PlayerService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private final MatchmakingStorage storage;
    private final PlayerEventPublisher playerEventPublisher;
    private final JsonMapper jsonMapper;

    public PlayerService(MatchmakingStorage storage,
                         PlayerEventPublisher playerEventPublisher,
                         JsonMapper jsonMapper) {
        this.storage = storage;
        this.playerEventPublisher = playerEventPublisher;
        this.jsonMapper = jsonMapper;
    }

    @PostConstruct
    public void initDemoPlayers() {
        createPlayerInternal(new PlayerRequest("Artem", 1210, Region.RU), false);
        createPlayerInternal(new PlayerRequest("Misha", 1180, Region.RU), false);
        createPlayerInternal(new PlayerRequest("Sasha", 1270, Region.RU), false);
        createPlayerInternal(new PlayerRequest("Dima", 1600, Region.RU), false);
    }

    @RabbitListener(queues = RabbitMQConfig.MATCH_ENRICHED_QUEUE, messageConverter = "")
    @SuppressWarnings("UseSpecificCatch")
    public void handleMatchEnriched(Message message) {
        try {
            JsonNode root = jsonMapper.readTree(message.getBody());
            EventMetadata metadata = jsonMapper.treeToValue(root.get("metadata"), EventMetadata.class);
            MatchEvent.Enriched enriched = jsonMapper.treeToValue(root.get("payload"), MatchEvent.Enriched.class);

            for (MatchEvent.PlayerRankChange change : enriched.changes()) {
                PlayerState oldPlayer = storage.players.get(change.playerId());
                if (oldPlayer == null) {
                    continue;
                }

                PlayerState updatedPlayer = new PlayerState(
                        oldPlayer.id(),
                        oldPlayer.nickname(),
                        change.newRating(),
                        oldPlayer.region(),
                        Rank.valueOf(change.newRank()),
                        oldPlayer.createdAt()
                );
                storage.players.put(updatedPlayer.id(), updatedPlayer);
            }

            log.info("Рейтинги игроков обновлены после match.enriched: matchId={} [eventId={}]",
                    enriched.matchId(), metadata.eventId());
        } catch (Exception e) {
            log.error("Не удалось обработать match.enriched: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать match.enriched", e);
        }
    }

    public PagedResponse<PlayerResponse> findPlayers(Region region, Rank rank, int page, int size) {
        var all = storage.players.values().stream()
                .filter(player -> region == null || player.region() == region)
                .filter(player -> rank == null || player.rank() == rank)
                .sorted(Comparator.comparing(PlayerState::createdAt).reversed())
                .map(this::toPlayerResponse)
                .toList();
        return PagedResponse.of(all, page, size);
    }

    public PlayerResponse findPlayerById(UUID id) {
        return toPlayerResponse(playerState(id));
    }

    public PlayerResponse createPlayer(PlayerRequest request) {
        return createPlayerInternal(request, true);
    }

    public void deletePlayer(UUID id) {
        playerState(id);

        boolean inWaitingLobby = storage.lobbies.values().stream()
                .anyMatch(lobby -> lobby.getStatus() == LobbyStatus.WAITING && lobby.getPlayerIds().contains(id));
        if (inWaitingLobby) {
            throw new InvalidLobbyOperationException("Нельзя удалить игрока, который находится в WAITING-лобби");
        }

        boolean inActiveMatch = storage.matches.values().stream()
                .anyMatch(match -> match.status() == MatchStatus.IN_PROGRESS && match.playerIds().contains(id));
        if (inActiveMatch) {
            throw new InvalidLobbyOperationException("Нельзя удалить игрока, который находится в активном матче");
        }

        storage.players.remove(id);
    }

    public PlayerState playerState(UUID id) {
        PlayerState player = storage.players.get(id);
        if (player == null) {
            throw new ResourceNotFoundException("Игрок не найден: " + id);
        }
        return player;
    }

    public PlayerResponse toPlayerResponse(PlayerState player) {
        return new PlayerResponse(player.id(), player.nickname(), player.rating(), player.region(), player.rank(), player.createdAt());
    }

    public Rank rankByRating(int rating) {
        if (rating < 1000) {
            return Rank.BRONZE;
        }
        if (rating < 1500) {
            return Rank.SILVER;
        }
        if (rating < 2000) {
            return Rank.GOLD;
        }
        if (rating < 2500) {
            return Rank.PLATINUM;
        }
        return Rank.DIAMOND;
    }

    private PlayerResponse createPlayerInternal(PlayerRequest request, boolean publishEvent) {
        Region region = request.region() == null ? Region.RU : request.region();
        int rating = request.rating() == null ? 1000 : request.rating();
        Rank rank = rankByRating(rating);

        PlayerState player = new PlayerState(
                UUID.randomUUID(),
                request.nickname(),
                rating,
                region,
                rank,
                OffsetDateTime.now()
        );
        storage.players.put(player.id(), player);

        PlayerResponse response = toPlayerResponse(player);
        if (publishEvent) {
            playerEventPublisher.publishCreated(response);
        }
        return response;
    }
}
