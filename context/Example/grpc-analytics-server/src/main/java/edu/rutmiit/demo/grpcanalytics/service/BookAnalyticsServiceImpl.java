package edu.rutmiit.demo.grpcanalytics.service;

import edu.rutmiit.demo.grpc.AnalyzeBookRequest;
import edu.rutmiit.demo.grpc.BookAnalysisResponse;
import edu.rutmiit.demo.grpc.BookAnalyticsGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Реализация gRPC-сервиса BookAnalytics.
 *
 * Наследует сгенерированный базовый класс BookAnalyticsImplBase —
 * аналог того, как REST-контроллер реализует интерфейс контракта:
 *
 *   REST:    AuthorController implements AuthorApi
 *   GraphQL: BookDataFetcher с @DgsQuery
 *   gRPC:    BookAnalyticsServiceImpl extends BookAnalyticsGrpc.BookAnalyticsImplBase
 *
 * Ключевые отличия от REST/GraphQL:
 * - Бинарный протокол (protobuf) вместо JSON — компактнее и быстрее
 * - Строго типизированный контракт (.proto) — несовместимость обнаруживается при компиляции
 * - HTTP/2 с мультиплексированием — несколько запросов в одном TCP-соединении
 * - Поддержка streaming (server, client, bidirectional) — здесь используем unary (простой запрос-ответ)
 */
public class BookAnalyticsServiceImpl extends BookAnalyticsGrpc.BookAnalyticsImplBase {

    private static final Logger log = LoggerFactory.getLogger(BookAnalyticsServiceImpl.class);

    /**
     * Обрабатывает запрос на анализ книги.
     *
     * Паттерн gRPC: метод получает request и StreamObserver для ответа.
     * StreamObserver — это callback-интерфейс:
     *   - onNext(response) — отправить ответ (для unary RPC вызывается один раз)
     *   - onCompleted()    — завершить RPC
     *   - onError(t)       — сообщить об ошибке
     *
     * Для unary RPC (один запрос → один ответ) всегда:
     *   responseObserver.onNext(response);
     *   responseObserver.onCompleted();
     */
    @Override
    public void analyzeBook(AnalyzeBookRequest request,
                            StreamObserver<BookAnalysisResponse> responseObserver) {

        log.info("gRPC запрос: анализ книги id={} «{}» (жанр: {}, год: {})",
                request.getBookId(), request.getTitle(),
                request.getGenre(), request.getPublishedYear());

        // ─── Вычисление метрик (демонстрационная логика) ─────────────
        int readingMinutes = estimateReadingTime(request.getGenre(), request.getPublishedYear());
        String difficulty = classifyDifficulty(request.getGenre(), request.getPublishedYear());
        double score = calculateRecommendationScore(request.getGenre(), request.getPublishedYear());
        String era = classifyEra(request.getPublishedYear());

        // ─── Формируем ответ ─────────────────────────────────────────
        BookAnalysisResponse response = BookAnalysisResponse.newBuilder()
                .setBookId(request.getBookId())
                .setEstimatedReadingMinutes(readingMinutes)
                .setDifficultyLevel(difficulty)
                .setRecommendationScore(score)
                .setEraClassification(era)
                .build();

        log.info("gRPC ответ: книга id={}, время чтения={}мин, сложность={}, балл={}, эпоха={}",
                response.getBookId(), readingMinutes, difficulty, score, era);

        // Отправляем ответ клиенту и завершаем RPC
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // ─── Демонстрационная бизнес-логика ──────────────────────────────

    /**
     * Оценка времени чтения на основе жанра.
     * В реальном приложении — ML-модель или API внешнего сервиса.
     */
    private int estimateReadingTime(String genre, int publishedYear) {
        int base = switch (genre != null ? genre.toLowerCase() : "") {
            case "роман", "novel"       -> 720;   // ~12 часов
            case "поэма", "poem"        -> 180;   // ~3 часа
            case "пьеса", "drama"       -> 240;   // ~4 часа
            case "рассказ", "story"     -> 60;    // ~1 час
            case "эпопея", "epic"       -> 1440;  // ~24 часа
            default                     -> 360;   // ~6 часов
        };
        // Классика читается медленнее (архаичный язык)
        if (publishedYear > 0 && publishedYear < 1900) {
            base = (int) (base * 1.3);
        }
        return base;
    }

    /**
     * Классификация сложности текста.
     */
    private String classifyDifficulty(String genre, int publishedYear) {
        if (publishedYear > 0 && publishedYear < 1800) return "CLASSIC";
        if (publishedYear >= 1800 && publishedYear < 1950) return "HARD";
        if (publishedYear >= 1950 && publishedYear < 2000) return "MEDIUM";
        return "EASY";
    }

    /**
     * Рекомендательный балл (0.0—10.0).
     * Демонстрационная формула: классика получает высокий балл.
     */
    private double calculateRecommendationScore(String genre, int publishedYear) {
        double base = 7.0;
        if (publishedYear > 0 && publishedYear < 1900) base += 1.5;   // классика
        if ("роман".equalsIgnoreCase(genre) || "novel".equalsIgnoreCase(genre)) base += 0.5;
        if ("эпопея".equalsIgnoreCase(genre) || "epic".equalsIgnoreCase(genre)) base += 1.0;
        return Math.min(base, 10.0);
    }

    /**
     * Классификация эпохи по году публикации.
     */
    private String classifyEra(int publishedYear) {
        if (publishedYear <= 0) return "UNKNOWN";
        if (publishedYear < 1600) return "ANCIENT";
        if (publishedYear < 1900) return "CLASSICAL";
        if (publishedYear < 2000) return "MODERN";
        return "CONTEMPORARY";
    }
}
