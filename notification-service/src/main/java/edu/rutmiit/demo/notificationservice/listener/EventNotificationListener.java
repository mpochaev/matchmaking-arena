package edu.rutmiit.demo.notificationservice.listener;

import edu.rutmiit.demo.matchmakingevents.EventMetadata;
import edu.rutmiit.demo.matchmakingevents.LobbyEvent;
import edu.rutmiit.demo.matchmakingevents.MatchEvent;
import edu.rutmiit.demo.matchmakingevents.PlayerEvent;
import edu.rutmiit.demo.matchmakingevents.RoutingKeys;
import edu.rutmiit.demo.notificationservice.config.RabbitMQConfig;
import edu.rutmiit.demo.notificationservice.websocket.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationListener.class);

    private final NotificationWebSocketHandler webSocketHandler;
    private final JsonMapper jsonMapper;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public EventNotificationListener(NotificationWebSocketHandler webSocketHandler, JsonMapper jsonMapper) {
        this.webSocketHandler = webSocketHandler;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE, messageConverter = "")
    public void handleEvent(Message message) {
        try {
            JsonNode root = jsonMapper.readTree(message.getBody());
            EventMetadata metadata = jsonMapper.treeToValue(root.get("metadata"), EventMetadata.class);

            if (!processedEventIds.add(metadata.eventId())) {
                log.warn("Повторное уведомление пропущено: eventId={}", metadata.eventId());
                return;
            }

            JsonNode payload = root.get("payload");
            String title = buildTitle(metadata.eventType());
            String description = buildDescription(metadata.eventType(), payload);
            String icon = resolveIcon(metadata.eventType());
            String level = resolveLevel(metadata.eventType());

            String json = jsonMapper.writeValueAsString(new NotificationPayload(
                    "NOTIFICATION",
                    metadata.eventId(),
                    metadata.eventType(),
                    title,
                    description,
                    icon,
                    level,
                    metadata.source(),
                    metadata.timestamp().toString(),
                    Instant.now().toString()
            ));

            webSocketHandler.broadcast(json);
            log.info("[УВЕДОМЛЕНИЕ] {} | {} (клиентов={})",
                    metadata.eventType(), description, webSocketHandler.getActiveConnectionCount());
        } catch (Exception e) {
            log.error("Ошибка обработки уведомления: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать уведомление", e);
        }
    }

    private String buildTitle(String eventType) {
        return switch (eventType) {
            case RoutingKeys.PLAYER_CREATED -> "Игрок создан";
            case RoutingKeys.LOBBY_CREATED -> "Лобби создано";
            case RoutingKeys.LOBBY_PLAYER_JOINED -> "Игрок вошел в лобби";
            case RoutingKeys.LOBBY_FORMED -> "Лобби сформировано";
            case RoutingKeys.LOBBY_DISBANDED -> "Лобби распущено";
            case RoutingKeys.MATCH_PROGRESS -> "Событие матча";
            case RoutingKeys.MATCH_FINISHED -> "Матч завершен";
            case RoutingKeys.MATCH_ENRICHED -> "Рейтинг пересчитан";
            default -> "Событие: " + eventType;
        };
    }

    private String buildDescription(String eventType, JsonNode payload) {
        try {
            return switch (eventType) {
                case RoutingKeys.PLAYER_CREATED -> {
                    PlayerEvent.Created e = jsonMapper.treeToValue(payload, PlayerEvent.Created.class);
                    yield "%s создан, рейтинг=%d, регион=%s, ранг=%s"
                            .formatted(e.nickname(), e.rating(), e.region(), e.rank());
                }
                case RoutingKeys.LOBBY_CREATED -> {
                    LobbyEvent.Created e = jsonMapper.treeToValue(payload, LobbyEvent.Created.class);
                    yield "Лобби %s/%s/%s создано, минимум игроков=%d"
                            .formatted(e.mode(), e.region(), e.rank(), e.minPlayers());
                }
                case RoutingKeys.LOBBY_PLAYER_JOINED -> {
                    LobbyEvent.PlayerJoined e = jsonMapper.treeToValue(payload, LobbyEvent.PlayerJoined.class);
                    yield "%s вошел в лобби %s (%d/%d)"
                            .formatted(e.nickname(), e.lobbyId(), e.playerCount(), e.minPlayers());
                }
                case RoutingKeys.LOBBY_FORMED -> {
                    LobbyEvent.Formed e = jsonMapper.treeToValue(payload, LobbyEvent.Formed.class);
                    yield "Лобби %s сформировано, матч=%s, игроков=%d: %s"
                            .formatted(e.lobbyId(), e.matchId(), e.playerCount(), e.playersSummary());
                }
                case RoutingKeys.LOBBY_DISBANDED -> {
                    LobbyEvent.Disbanded e = jsonMapper.treeToValue(payload, LobbyEvent.Disbanded.class);
                    yield "Лобби %s распущено, причина=%s, игроков=%d"
                            .formatted(e.lobbyId(), e.reason(), e.playerCount());
                }
                case RoutingKeys.MATCH_PROGRESS -> {
                    MatchEvent.Progress e = jsonMapper.treeToValue(payload, MatchEvent.Progress.class);
                    yield "Матч %s: [%s] %s"
                            .formatted(e.matchId(), e.eventType(), e.message());
                }
                case RoutingKeys.MATCH_FINISHED -> {
                    MatchEvent.Finished e = jsonMapper.treeToValue(payload, MatchEvent.Finished.class);
                    yield "Матч %s завершен, победитель=%s, игроков=%d"
                            .formatted(e.matchId(), e.winnerNickname(), e.playerCount());
                }
                case RoutingKeys.MATCH_ENRICHED -> {
                    MatchEvent.Enriched e = jsonMapper.treeToValue(payload, MatchEvent.Enriched.class);
                    String changes = e.changes().stream()
                            .map(change -> "%s: %d до %d (%+d)"
                                    .formatted(change.nickname(), change.oldRating(), change.newRating(), change.delta()))
                            .reduce((left, right) -> left + "; " + right)
                            .orElse("изменений рейтинга нет");
                    yield "gRPC-обогащение матча %s: победитель=%s. %s"
                            .formatted(e.matchId(), e.winnerNickname(), changes);
                }
                default -> "Неизвестные данные события";
            };
        } catch (Exception e) {
            return "Не удалось прочитать данные уведомления: " + e.getMessage();
        }
    }

    private String resolveIcon(String eventType) {
        return switch (eventType) {
            case RoutingKeys.PLAYER_CREATED -> "player";
            case RoutingKeys.LOBBY_CREATED, RoutingKeys.LOBBY_PLAYER_JOINED,
                 RoutingKeys.LOBBY_FORMED, RoutingKeys.LOBBY_DISBANDED -> "lobby";
            case RoutingKeys.MATCH_PROGRESS, RoutingKeys.MATCH_FINISHED -> "match";
            case RoutingKeys.MATCH_ENRICHED -> "rank";
            default -> "event";
        };
    }

    private String resolveLevel(String eventType) {
        return switch (eventType) {
            case RoutingKeys.MATCH_FINISHED, RoutingKeys.MATCH_ENRICHED -> "success";
            case RoutingKeys.MATCH_PROGRESS -> "info";
            case RoutingKeys.LOBBY_DISBANDED -> "warning";
            default -> "info";
        };
    }

    public record NotificationPayload(
            String type,
            String eventId,
            String eventType,
            String title,
            String description,
            String icon,
            String level,
            String source,
            String eventTimestamp,
            String receivedAt
    ) {}
}
