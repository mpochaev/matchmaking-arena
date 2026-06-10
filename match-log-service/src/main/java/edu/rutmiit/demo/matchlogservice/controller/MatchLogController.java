package edu.rutmiit.demo.matchlogservice.controller;

import edu.rutmiit.demo.matchlogservice.model.MatchLogEntry;
import edu.rutmiit.demo.matchlogservice.storage.MatchLogStorage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** REST-эндпоинт для просмотра матч-лога. */
@RestController
@RequestMapping("/api/match-log")
public class MatchLogController {

    private final MatchLogStorage matchLogStorage;

    public MatchLogController(MatchLogStorage matchLogStorage) {
        this.matchLogStorage = matchLogStorage;
    }

    @GetMapping
    public Map<String, Object> getMatchLog(@RequestParam(defaultValue = "100") int limit) {
        List<MatchLogEntry> entries = matchLogStorage.findLatest(limit);
        return Map.of(
                "totalEntries", matchLogStorage.count(),
                "showing", entries.size(),
                "entries", entries
        );
    }
}
