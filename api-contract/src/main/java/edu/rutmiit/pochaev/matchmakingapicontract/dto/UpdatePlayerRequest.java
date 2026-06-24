package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePlayerRequest(
        @NotBlank String nickname,
        @NotNull @Min(0) @Max(5000) Integer rating,
        @NotNull Region region
) {}
