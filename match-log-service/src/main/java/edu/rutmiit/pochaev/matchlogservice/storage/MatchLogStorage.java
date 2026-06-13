package edu.rutmiit.pochaev.matchlogservice.storage;

import edu.rutmiit.pochaev.matchlogservice.model.MatchLogEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/** Простое in-memory хранилище матч-лога. */
@Component
public class MatchLogStorage {

    private final ConcurrentLinkedDeque<MatchLogEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicLong sequence = new AtomicLong(0);
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public boolean isDuplicate(String eventId) {
        return !processedEventIds.add(eventId);
    }

    public MatchLogEntry save(MatchLogEntry entry) {
        MatchLogEntry numbered = new MatchLogEntry(
                sequence.incrementAndGet(),
                entry.eventId(),
                entry.eventType(),
                entry.source(),
                entry.eventTimestamp(),
                entry.receivedAt(),
                entry.description()
        );
        entries.addFirst(numbered);
        return numbered;
    }

    public List<MatchLogEntry> findLatest(int limit) {
        return entries.stream().limit(limit).toList();
    }

    public int count() {
        return entries.size();
    }
}
