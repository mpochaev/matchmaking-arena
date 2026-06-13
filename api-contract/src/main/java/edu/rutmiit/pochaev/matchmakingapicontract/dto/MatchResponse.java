package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        UUID lobbyId,
        String mode,
        String region,
        String rank,
        String status,
        List<PlayerResponse> players,
        List<MatchEventResponse> events,
        PlayerResponse winner,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt
) {}
