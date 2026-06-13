package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record JoinLobbyRequest(
        @NotNull UUID playerId,
        @NotBlank String mode,
        @NotBlank String region,
        @NotBlank String rank,
        @Min(5) Integer timeoutSeconds
) {}
