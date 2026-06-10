package edu.rutmiit.demo.demorest.event;

import edu.rutmiit.demo.booksapicontract.dto.BookResponse;
import edu.rutmiit.demo.events.BookEvent;
import edu.rutmiit.demo.events.EventEnvelope;
import edu.rutmiit.demo.events.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Публикация доменных событий книг в RabbitMQ.
 *
 * Паттерн: BookService вызывает publish-метод ПОСЛЕ успешного завершения
 * бизнес-операции. Если RabbitMQ недоступен — событие логируется как ошибка,
 * но основная операция (создание/удаление книги) НЕ откатывается.
 *
 * Это паттерн «fire-and-forget» — допустимая потеря события лучше,
 * чем отказ бизнес-операции из-за недоступности брокера.
 *
 * В промышленных системах для гарантированной доставки используют:
 * - Transactional Outbox (запись события в БД в одной транзакции с данными),
 * - Change Data Capture (Debezium/Kafka Connect).
 */
@Component
public class BookEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookEventPublisher.class);
    private static final String SOURCE = "demo-rest";

    private final RabbitTemplate rabbitTemplate;

    public BookEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Публикует событие «книга создана».
     */
    public void publishCreated(BookResponse book) {
        var event = new BookEvent.Created(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getAuthor() != null ? book.getAuthor().getId() : null,
                book.getAuthor() != null ? book.getAuthor().getFullName() : "Неизвестен",
                book.getGenre(),
                book.getPublishedYear()
        );
        send(RoutingKeys.BOOK_CREATED, event);
    }

    /**
     * Публикует событие «книга обновлена».
     */
    public void publishUpdated(BookResponse book) {
        var event = new BookEvent.Updated(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getGenre(),
                book.getPublishedYear()
        );
        send(RoutingKeys.BOOK_UPDATED, event);
    }

    /**
     * Публикует событие «книга удалена».
     */
    public void publishDeleted(Long bookId, String title) {
        var event = new BookEvent.Deleted(bookId, title);
        send(RoutingKeys.BOOK_DELETED, event);
    }

    /**
     * Отправляет событие в RabbitMQ, обёрнутое в EventEnvelope.
     *
     * convertAndSend:
     * - 1й аргумент: имя exchange
     * - 2й аргумент: routing key (определяет, в какие очереди попадёт сообщение)
     * - 3й аргумент: объект, который Jackson сериализует в JSON
     *
     * try-catch гарантирует, что ошибка публикации не сломает основной бизнес-поток.
     */
    private void send(String routingKey, BookEvent event) {
        try {
            EventEnvelope<BookEvent> envelope = EventEnvelope.wrap(event, SOURCE, routingKey);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, envelope);
            log.info("Событие отправлено: {} [eventId={}]", routingKey, envelope.metadata().eventId());
        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}", routingKey, e.getMessage());
        }
    }
}
