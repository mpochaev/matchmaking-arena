package edu.rutmiit.pochaev.matchmakingapicontract.endpoints;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.PatchPlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.UpdatePlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
            @RequestParam(required = false) Region region,
            @RequestParam(required = false) Rank rank,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "Получить игрока по ID")
    @GetMapping("/{id}")
    EntityModel<PlayerResponse> getPlayerById(@Parameter(description = "ID игрока") @PathVariable UUID id);

    @Operation(summary = "Создать игрока")
    @PostMapping
    ResponseEntity<EntityModel<PlayerResponse>> createPlayer(@Valid @RequestBody PlayerRequest request);


    @Operation(summary = "Полностью обновить игрока")
    @PutMapping("/{id}")
    EntityModel<PlayerResponse> updatePlayer(
            @Parameter(description = "ID игрока") @PathVariable UUID id,
            @Valid @RequestBody UpdatePlayerRequest request
    );

    @Operation(summary = "Частично обновить игрока")
    @PatchMapping("/{id}")
    EntityModel<PlayerResponse> patchPlayer(
            @Parameter(description = "ID игрока") @PathVariable UUID id,
            @Valid @RequestBody PatchPlayerRequest request
    );

    @Operation(summary = "Удалить игрока")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deletePlayer(@Parameter(description = "ID игрока") @PathVariable UUID id);
}
