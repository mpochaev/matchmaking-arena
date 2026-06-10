package edu.rutmiit.demo.auditservice.listener;

import edu.rutmiit.demo.auditservice.model.AuditEntry;
import edu.rutmiit.demo.auditservice.storage.AuditStorage;
import edu.rutmiit.demo.events.AuthorEvent;
import edu.rutmiit.demo.events.BookEvent;
import edu.rutmiit.demo.events.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * Единый слушатель всех доменных событий из RabbitMQ.
 *
 * Принимает «сырое» AMQP-сообщение (Message) и десериализует его вручную.
 * Это необходимо, потому что EventEnvelope<T> — generic тип, и Jackson
 * не может определить конкретный подтип T при автоматической десериализации.
 *
 * Промышленная альтернатива:
 * - отдельные очереди для разных типов событий (не generic listener),
 * - Spring Cloud Stream с content-type routing,
 * - Apache Avro/Protobuf с Schema Registry.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditStorage auditStorage;
    private final JsonMapper jsonMapper;

    public AuditEventListener(AuditStorage auditStorage, JsonMapper jsonMapper) {
        this.auditStorage = auditStorage;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Принимает все события из очереди q.audit.events.
     *
     * Десериализация выполняется в два этапа:
     * 1. Парсим JSON в дерево узлов (JsonNode) — быстро и безопасно.
     * 2. Извлекаем metadata и определяем тип payload по полю eventType.
     * 3. Десериализуем payload в конкретный record по выявленному типу.
     */
    @RabbitListener(queues = "q.audit.events", messageConverter = "")
    public void handleEvent(Message message) {
        try {
            byte[] body = message.getBody();
            JsonNode root = jsonMapper.readTree(body);

            // Извлекаем метаданные из JSON-конверта
            JsonNode metaNode = root.get("metadata");
            EventMetadata metadata = jsonMapper.treeToValue(metaNode, EventMetadata.class);

            // Дедупликация — если событие уже обработано, пропускаем
            if (auditStorage.isDuplicate(metadata.eventId())) {
                log.warn("Дубликат события пропущен: eventId={}", metadata.eventId());
                return;
            }

            // Определяем тип события и формируем описание
            JsonNode payloadNode = root.get("payload");
            String description = buildDescription(metadata.eventType(), payloadNode);

            AuditEntry entry = auditStorage.save(new AuditEntry(
                    0,
                    metadata.eventId(),
                    metadata.eventType(),
                    metadata.source(),
                    metadata.timestamp(),
                    Instant.now(),
                    description
            ));

            log.info("[AUDIT #{}] {} | {}", entry.sequenceNumber(), metadata.eventType(), description);

        } catch (Exception e) {
            log.error("Ошибка обработки события: {}", e.getMessage(), e);
            // Исключение пробросится, сообщение уйдёт в DLQ после исчерпания retries
            throw new RuntimeException("Не удалось обработать событие", e);
        }
    }

    /**
     * Формирует человекочитаемое описание события для аудит-лога.
     *
     * Десериализует payload в конкретный тип на основе eventType,
     * затем формирует описание через pattern matching по sealed interface.
     */
    private String buildDescription(String eventType, JsonNode payloadNode) throws Exception {
        return switch (eventType) {
            case "book.created" -> {
                BookEvent.Created e = jsonMapper.treeToValue(payloadNode, BookEvent.Created.class);
                yield String.format("Создана книга «%s» (ISBN: %s), автор: %s",
                        e.title(), e.isbn(), e.authorFullName());
            }
            case "book.updated" -> {
                BookEvent.Updated e = jsonMapper.treeToValue(payloadNode, BookEvent.Updated.class);
                yield String.format("Обновлена книга id=%d «%s»", e.bookId(), e.title());
            }
            case "book.deleted" -> {
                BookEvent.Deleted e = jsonMapper.treeToValue(payloadNode, BookEvent.Deleted.class);
                yield String.format("Удалена книга id=%d «%s»", e.bookId(), e.title());
            }
            case "author.created" -> {
                AuthorEvent.Created e = jsonMapper.treeToValue(payloadNode, AuthorEvent.Created.class);
                yield String.format("Создан автор «%s» (национальность: %s)",
                        e.fullName(), e.nationality());
            }
            case "author.deleted" -> {
                AuthorEvent.Deleted e = jsonMapper.treeToValue(payloadNode, AuthorEvent.Deleted.class);
                yield String.format("Удалён автор «%s» (удалено книг: %d)",
                        e.fullName(), e.deletedBooksCount());
            }
            case "book.enriched" -> {
                BookEvent.Enriched e = jsonMapper.treeToValue(payloadNode, BookEvent.Enriched.class);
                yield String.format("Книга обогащена id=%d «%s» (время чтения: %dмин, сложность: %s, балл: %.1f, эпоха: %s)",
                        e.bookId(), e.title(), e.estimatedReadingMinutes(),
                        e.difficultyLevel(), e.recommendationScore(), e.eraClassification());
            }
            default -> "Неизвестное событие: " + eventType;
        };
    }
}
