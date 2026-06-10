package edu.rutmiit.demo.matchlogservice.model;

import java.time.Instant;

/** Одна запись матч-лога. */
public record MatchLogEntry(
        long sequenceNumber,
        String eventId,
        String eventType,
        String source,
        Instant eventTimestamp,
        Instant receivedAt,
        String description
) {}
