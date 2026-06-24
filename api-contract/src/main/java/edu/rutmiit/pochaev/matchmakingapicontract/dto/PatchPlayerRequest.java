package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record PatchPlayerRequest(
        @Pattern(regexp = ".*\\S.*", message = "nickname must not be blank") String nickname,
        @Min(0) @Max(5000) Integer rating,
        Region region
) {}
