package com.example.demo.event;

import edu.rutmiit.demo.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.demo.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.demo.matchmakingevents.EventEnvelope;
import edu.rutmiit.demo.matchmakingevents.LobbyEvent;
import edu.rutmiit.demo.matchmakingevents.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

/** Публикация событий лобби. */
@Component
public class LobbyEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LobbyEventPublisher.class);
    private static final String SOURCE = "matchmaking";

    private final RabbitTemplate rabbitTemplate;

    public LobbyEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCreated(LobbyResponse lobby) {
        var event = new LobbyEvent.Created(
                lobby.id(),
                lobby.mode(),
                lobby.region(),
                lobby.rank(),
                lobby.minPlayers(),
                lobby.deadlineAt()
        );
        send(RoutingKeys.LOBBY_CREATED, event);
    }

    public void publishPlayerJoined(LobbyResponse lobby, PlayerResponse player) {
        var event = new LobbyEvent.PlayerJoined(
                lobby.id(),
                player.id(),
                player.nickname(),
                lobby.playerCount(),
                lobby.minPlayers()
        );
        send(RoutingKeys.LOBBY_PLAYER_JOINED, event);
    }

    public void publishFormed(LobbyResponse lobby, UUID matchId) {
        String playersSummary = lobby.players().stream()
                .map(player -> player.nickname())
                .collect(Collectors.joining(", "));

        var event = new LobbyEvent.Formed(
                lobby.id(),
                matchId,
                lobby.playerCount(),
                playersSummary
        );
        send(RoutingKeys.LOBBY_FORMED, event);
    }

    public void publishDisbanded(LobbyResponse lobby, String reason) {
        var event = new LobbyEvent.Disbanded(
                lobby.id(),
                reason,
                lobby.playerCount()
        );
        send(RoutingKeys.LOBBY_DISBANDED, event);
    }

    private void send(String routingKey, LobbyEvent event) {
        try {
            EventEnvelope<LobbyEvent> envelope = EventEnvelope.wrap(event, SOURCE, routingKey);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, envelope);
            log.info("Событие отправлено: {} [eventId={}]", routingKey, envelope.metadata().eventId());
        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}", routingKey, e.getMessage());
        }
    }
}
