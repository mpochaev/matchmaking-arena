package edu.rutmiit.pochaev.matchmakingevents;

/**
 * Конверт события. metadata нужны инфраструктуре, payload это бизнес-данные.
 */
public record EventEnvelope<T>(
        EventMetadata metadata,
        T payload
) {
    public static <T> EventEnvelope<T> wrap(T payload, String source, String eventType) {
        return new EventEnvelope<>(EventMetadata.create(source, eventType), payload);
    }
}
