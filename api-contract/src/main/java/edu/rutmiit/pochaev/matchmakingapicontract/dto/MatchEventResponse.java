package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchEventResponse(
        UUID id,
        String type,
        String message,
        UUID actorPlayerId,
        UUID targetPlayerId,
        OffsetDateTime occurredAt
) {}
