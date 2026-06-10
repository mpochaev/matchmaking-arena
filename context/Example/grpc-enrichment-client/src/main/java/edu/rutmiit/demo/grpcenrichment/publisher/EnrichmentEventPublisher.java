package edu.rutmiit.demo.grpcenrichment.publisher;

import edu.rutmiit.demo.events.BookEvent;
import edu.rutmiit.demo.events.EventEnvelope;
import edu.rutmiit.demo.events.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Публикация событий обогащения (book.enriched) в RabbitMQ.
 *
 * Аналогичен BookEventPublisher в demo-rest, но публикует другой тип события.
 * Паттерн fire-and-forget: если RabbitMQ недоступен, ошибка логируется,
 * но gRPC-вызов уже выполнен — результат не теряется полностью.
 */
@Component
public class EnrichmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentEventPublisher.class);
    private static final String SOURCE = "grpc-enrichment-client";

    private final RabbitTemplate rabbitTemplate;

    public EnrichmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Публикует событие book.enriched с результатами gRPC-аналитики.
     */
    public void publishEnriched(BookEvent.Enriched enrichedEvent) {
        try {
            EventEnvelope<BookEvent> envelope = EventEnvelope.wrap(
                    enrichedEvent, SOURCE, RoutingKeys.BOOK_ENRICHED);

            rabbitTemplate.convertAndSend(
                    RoutingKeys.EXCHANGE,
                    RoutingKeys.BOOK_ENRICHED,
                    envelope);

            log.info("Событие отправлено: {} [bookId={}, eventId={}]",
                    RoutingKeys.BOOK_ENRICHED,
                    enrichedEvent.bookId(),
                    envelope.metadata().eventId());

        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}",
                    RoutingKeys.BOOK_ENRICHED, e.getMessage());
        }
    }
}
