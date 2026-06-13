package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlayerRequest(
        @NotBlank String nickname,
        @Min(0) @Max(5000) Integer rating,
        String region,
        String rank
) {}
