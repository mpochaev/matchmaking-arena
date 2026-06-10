package edu.rutmiit.demo.grpcenrichment.listener;

import edu.rutmiit.demo.events.BookEvent;
import edu.rutmiit.demo.events.EventMetadata;
import edu.rutmiit.demo.grpc.AnalyzeBookRequest;
import edu.rutmiit.demo.grpc.BookAnalysisResponse;
import edu.rutmiit.demo.grpc.BookAnalyticsGrpc;
import edu.rutmiit.demo.grpcenrichment.publisher.EnrichmentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Слушатель событий book.created из RabbitMQ.
 *
 * Десериализация — ручная (как в audit-service), потому что EventEnvelope<T>
 * является generic-типом, и Jackson не может определить конкретный подтип T.
 */
@Component
public class BookCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(BookCreatedListener.class);

    private final BookAnalyticsGrpc.BookAnalyticsBlockingStub analyticsStub;
    private final EnrichmentEventPublisher enrichmentPublisher;
    private final JsonMapper jsonMapper;

    public BookCreatedListener(BookAnalyticsGrpc.BookAnalyticsBlockingStub analyticsStub,
                               EnrichmentEventPublisher enrichmentPublisher,
                               JsonMapper jsonMapper) {
        this.analyticsStub = analyticsStub;
        this.enrichmentPublisher = enrichmentPublisher;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Обрабатывает событие book.created:
     * 1. Десериализует событие из JSON
     * 2. Формирует gRPC-запрос
     * 3. Вызывает gRPC-сервер (синхронно)
     * 4. Публикует результат как событие book.enriched
     */
    @RabbitListener(queues = "q.enrichment.book-created", messageConverter = "")
    public void handleBookCreated(Message message) {
        try {
            // 1. Парсим JSON-конверт
            byte[] body = message.getBody();
            JsonNode root = jsonMapper.readTree(body);

            JsonNode metaNode = root.get("metadata");
            EventMetadata metadata = jsonMapper.treeToValue(metaNode, EventMetadata.class);

            JsonNode payloadNode = root.get("payload");
            BookEvent.Created bookCreated = jsonMapper.treeToValue(payloadNode, BookEvent.Created.class);

            log.info("Получено событие book.created: bookId={}, «{}» [eventId={}]",
                    bookCreated.bookId(), bookCreated.title(), metadata.eventId());

            // 2. Формируем gRPC-запрос
            AnalyzeBookRequest grpcRequest = AnalyzeBookRequest.newBuilder()
                    .setBookId(bookCreated.bookId())
                    .setTitle(bookCreated.title())
                    .setGenre(bookCreated.genre() != null ? bookCreated.genre() : "")
                    .setPublishedYear(bookCreated.publishedYear() != null ? bookCreated.publishedYear() : 0)
                    .setAuthorName(bookCreated.authorFullName() != null ? bookCreated.authorFullName() : "")
                    .build();

            // 3. Вызываем gRPC-сервер (синхронно)
            log.info("Вызов gRPC: BookAnalytics.AnalyzeBook(bookId={})", bookCreated.bookId());
            BookAnalysisResponse grpcResponse = analyticsStub.analyzeBook(grpcRequest);

            log.info("gRPC ответ получен: bookId={}, время={}мин, сложность={}, балл={}, эпоха={}",
                    grpcResponse.getBookId(),
                    grpcResponse.getEstimatedReadingMinutes(),
                    grpcResponse.getDifficultyLevel(),
                    grpcResponse.getRecommendationScore(),
                    grpcResponse.getEraClassification());

            // 4. Публикуем событие book.enriched
            BookEvent.Enriched enrichedEvent = new BookEvent.Enriched(
                    grpcResponse.getBookId(),
                    bookCreated.title(),
                    grpcResponse.getEstimatedReadingMinutes(),
                    grpcResponse.getDifficultyLevel(),
                    grpcResponse.getRecommendationScore(),
                    grpcResponse.getEraClassification()
            );

            enrichmentPublisher.publishEnriched(enrichedEvent);

            log.info("Книга обогащена: bookId={}, «{}» → book.enriched отправлено",
                    bookCreated.bookId(), bookCreated.title());

        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC ошибка при обогащении книги: {} ({})",
                    e.getStatus().getDescription(), e.getStatus().getCode());
            throw new RuntimeException("gRPC-вызов завершился ошибкой", e);

        } catch (Exception e) {
            log.error("Ошибка обработки события book.created: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать событие book.created", e);
        }
    }
}
