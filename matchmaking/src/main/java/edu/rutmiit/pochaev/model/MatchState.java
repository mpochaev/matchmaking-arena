package edu.rutmiit.pochaev.model;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchEventResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MatchState(
        UUID id,
        UUID lobbyId,
        MatchMode mode,
        Region region,
        Rank rank,
        MatchStatus status,
        List<UUID> playerIds,
        List<MatchEventResponse> events,
        UUID winnerPlayerId,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt
) {}
