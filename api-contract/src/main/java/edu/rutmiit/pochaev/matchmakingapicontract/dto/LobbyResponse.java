package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LobbyResponse(
        UUID id,
        String mode,
        String region,
        String rank,
        String status,
        int minPlayers,
        int playerCount,
        List<PlayerResponse> players,
        OffsetDateTime createdAt,
        OffsetDateTime deadlineAt,
        OffsetDateTime startedAt,
        UUID matchId
) {}
