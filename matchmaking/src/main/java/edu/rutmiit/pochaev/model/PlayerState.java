package edu.rutmiit.pochaev.model;

import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlayerState(
        UUID id,
        String nickname,
        int rating,
        Region region,
        Rank rank,
        OffsetDateTime createdAt
) {}
