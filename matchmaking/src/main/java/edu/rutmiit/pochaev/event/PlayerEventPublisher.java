package edu.rutmiit.pochaev.event;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.pochaev.matchmakingevents.EventEnvelope;
import edu.rutmiit.pochaev.matchmakingevents.PlayerEvent;
import edu.rutmiit.pochaev.matchmakingevents.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Публикация событий игроков. */
@Component
public class PlayerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PlayerEventPublisher.class);
    private static final String SOURCE = "matchmaking";

    private final RabbitTemplate rabbitTemplate;

    public PlayerEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCreated(PlayerResponse player) {
        var event = new PlayerEvent.Created(
                player.id(),
                player.nickname(),
                player.rating(),
                player.region().name(),
                player.rank().name()
        );
        send(RoutingKeys.PLAYER_CREATED, event);
    }

    private void send(String routingKey, PlayerEvent event) {
        try {
            EventEnvelope<PlayerEvent> envelope = EventEnvelope.wrap(event, SOURCE, routingKey);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, envelope);
            log.info("Событие отправлено: {} [eventId={}]", routingKey, envelope.metadata().eventId());
        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}", routingKey, e.getMessage());
        }
    }
}
