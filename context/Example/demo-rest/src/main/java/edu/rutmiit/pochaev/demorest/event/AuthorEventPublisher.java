package edu.rutmiit.pochaev.demorest.event;

import edu.rutmiit.pochaev.booksapicontract.dto.AuthorResponse;
import edu.rutmiit.pochaev.events.AuthorEvent;
import edu.rutmiit.pochaev.events.EventEnvelope;
import edu.rutmiit.pochaev.events.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Публикация доменных событий авторов в RabbitMQ.
 *
 * Аналогичен BookEventPublisher — тот же fire-and-forget паттерн.
 */
@Component
public class AuthorEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuthorEventPublisher.class);
    private static final String SOURCE = "demo-rest";

    private final RabbitTemplate rabbitTemplate;

    public AuthorEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Публикует событие «автор создан».
     */
    public void publishCreated(AuthorResponse author) {
        var event = new AuthorEvent.Created(
                author.getId(),
                author.getFirstName(),
                author.getLastName(),
                author.getFullName(),
                author.getNationality()
        );
        send(RoutingKeys.AUTHOR_CREATED, event);
    }

    /**
     * Публикует событие «автор удалён» с количеством каскадно удалённых книг.
     */
    public void publishDeleted(AuthorResponse author, int deletedBooksCount) {
        var event = new AuthorEvent.Deleted(
                author.getId(),
                author.getFullName(),
                deletedBooksCount
        );
        send(RoutingKeys.AUTHOR_DELETED, event);
    }

    private void send(String routingKey, AuthorEvent event) {
        try {
            EventEnvelope<AuthorEvent> envelope = EventEnvelope.wrap(event, SOURCE, routingKey);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, envelope);
            log.info("Событие отправлено: {} [eventId={}]", routingKey, envelope.metadata().eventId());
        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}", routingKey, e.getMessage());
        }
    }
}
