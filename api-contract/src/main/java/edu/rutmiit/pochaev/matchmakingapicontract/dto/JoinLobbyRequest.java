package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record JoinLobbyRequest(
        @NotNull UUID playerId,
        @NotNull MatchMode mode,
        @NotNull Region region,
        @Min(5) Integer timeoutSeconds
) {}
