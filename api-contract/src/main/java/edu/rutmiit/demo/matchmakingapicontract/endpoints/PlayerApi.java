package edu.rutmiit.demo.matchmakingapicontract.endpoints;

import edu.rutmiit.demo.matchmakingapicontract.dto.PlayerRequest;
import edu.rutmiit.demo.matchmakingapicontract.dto.PlayerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Игроки", description = "Игроки матчмейкинга")
@RequestMapping("/api/players")
public interface PlayerApi {

    @Operation(summary = "Получить список игроков")
    @GetMapping
    PagedModel<EntityModel<PlayerResponse>> getAllPlayers(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String rank,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "Получить игрока по ID")
    @GetMapping("/{id}")
    EntityModel<PlayerResponse> getPlayerById(@Parameter(description = "ID игрока") @PathVariable UUID id);

    @Operation(summary = "Создать игрока")
    @PostMapping
    ResponseEntity<EntityModel<PlayerResponse>> createPlayer(@Valid @RequestBody PlayerRequest request);
}
