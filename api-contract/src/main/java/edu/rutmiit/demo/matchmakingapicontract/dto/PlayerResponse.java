package edu.rutmiit.demo.matchmakingapicontract.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlayerResponse(
        UUID id,
        String nickname,
        int rating,
        String region,
        String rank,
        OffsetDateTime createdAt
) {}
