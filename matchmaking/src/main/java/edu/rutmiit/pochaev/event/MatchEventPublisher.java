package edu.rutmiit.pochaev.event;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchEventResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.matchmakingevents.EventEnvelope;
import edu.rutmiit.pochaev.matchmakingevents.MatchEvent;
import edu.rutmiit.pochaev.matchmakingevents.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Публикация событий матчей. */
@Component
public class MatchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MatchEventPublisher.class);
    private static final String SOURCE = "matchmaking";

    private final RabbitTemplate rabbitTemplate;

    public MatchEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }


    public void publishProgress(MatchResponse match, MatchEventResponse event) {
        var progress = new MatchEvent.Progress(
                match.id(),
                match.lobbyId(),
                event.type(),
                event.message(),
                match.events().size(),
                match.status()
        );
        send(RoutingKeys.MATCH_PROGRESS, progress);
    }

    public void publishFinished(MatchResponse match) {
        var players = match.players().stream()
                .map(player -> new MatchEvent.MatchPlayer(
                        player.id(),
                        player.nickname(),
                        player.rating(),
                        player.rank()
                ))
                .toList();

        var event = new MatchEvent.Finished(
                match.id(),
                match.lobbyId(),
                match.mode(),
                match.region(),
                match.rank(),
                match.players().size(),
                players,
                match.winner() == null ? null : match.winner().id(),
                match.winner() == null ? "Неизвестно" : match.winner().nickname(),
                buildResultSummary(match)
        );
        send(RoutingKeys.MATCH_FINISHED, event);
    }

    private String buildResultSummary(MatchResponse match) {
        String winner = match.winner() == null ? "Неизвестно" : match.winner().nickname();
        StringBuilder result = new StringBuilder("Итог матча: победитель = ")
                .append(winner)
                .append(". События: ");

        for (int i = 0; i < match.events().size(); i++) {
            var event = match.events().get(i);
            if (i > 0) {
                result.append(" | ");
            }
            result.append(event.message());
        }

        return result.toString();
    }

    private void send(String routingKey, MatchEvent event) {
        try {
            EventEnvelope<MatchEvent> envelope = EventEnvelope.wrap(event, SOURCE, routingKey);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, envelope);
            log.info("Событие отправлено: {} [eventId={}]", routingKey, envelope.metadata().eventId());
        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}", routingKey, e.getMessage());
        }
    }
}
