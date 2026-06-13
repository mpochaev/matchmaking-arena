package edu.rutmiit.pochaev.grpcmatchenrichment.publisher;

import edu.rutmiit.pochaev.matchmakingevents.EventEnvelope;
import edu.rutmiit.pochaev.matchmakingevents.MatchEvent;
import edu.rutmiit.pochaev.matchmakingevents.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Публикация события match.enriched обратно в RabbitMQ. */
@Component
public class MatchEnrichmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MatchEnrichmentEventPublisher.class);
    private static final String SOURCE = "grpc-match-enrichment-client";

    private final RabbitTemplate rabbitTemplate;

    public MatchEnrichmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishEnriched(MatchEvent.Enriched enrichedEvent) {
        try {
            EventEnvelope<MatchEvent> envelope = EventEnvelope.wrap(
                    enrichedEvent, SOURCE, RoutingKeys.MATCH_ENRICHED);

            rabbitTemplate.convertAndSend(
                    RoutingKeys.EXCHANGE,
                    RoutingKeys.MATCH_ENRICHED,
                    envelope);

            log.info("Событие отправлено: {} [matchId={}, eventId={}]",
                    RoutingKeys.MATCH_ENRICHED,
                    enrichedEvent.matchId(),
                    envelope.metadata().eventId());
        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}", RoutingKeys.MATCH_ENRICHED, e.getMessage());
        }
    }
}
