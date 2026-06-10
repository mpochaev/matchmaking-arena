package edu.rutmiit.demo.matchmakingevents;

import java.time.Instant;
import java.util.UUID;

/**
 * Метаданные события. id, время, источник и тип.
 * Упрощенный аналог CloudEvents для учебного проекта.
 */
public record EventMetadata(
        String eventId,
        Instant timestamp,
        String source,
        String eventType
) {
    public static EventMetadata create(String source, String eventType) {
        return new EventMetadata(
                UUID.randomUUID().toString(),
                Instant.now(),
                source,
                eventType
        );
    }
}
