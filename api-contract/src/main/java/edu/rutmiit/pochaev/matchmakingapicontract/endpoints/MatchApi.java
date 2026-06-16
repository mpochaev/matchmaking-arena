package edu.rutmiit.pochaev.matchmakingapicontract.endpoints;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Матчи", description = "Матчи, созданные из лобби")
@RequestMapping("/api/matches")
public interface MatchApi {

    @Operation(summary = "Получить список матчей")
    @GetMapping
    PagedModel<EntityModel<MatchResponse>> getAllMatches(
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(required = false) Region region,
            @RequestParam(required = false) Rank rank,
            @RequestParam(required = false) MatchMode mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "Получить матч по ID")
    @GetMapping("/{id}")
    EntityModel<MatchResponse> getMatchById(@Parameter(description = "ID матча") @PathVariable UUID id);
}
