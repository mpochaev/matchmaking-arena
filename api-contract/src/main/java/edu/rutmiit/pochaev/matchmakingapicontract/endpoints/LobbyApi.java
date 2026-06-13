package edu.rutmiit.pochaev.matchmakingapicontract.endpoints;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.JoinLobbyRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Лобби", description = "Лобби, где игроки ждут старта игры")
@RequestMapping("/api/lobbies")
public interface LobbyApi {

    @Operation(summary = "Получить список лобби")
    @GetMapping
    PagedModel<EntityModel<LobbyResponse>> getAllLobbies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) String mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "Получить лобби по ID")
    @GetMapping("/{id}")
    EntityModel<LobbyResponse> getLobbyById(@Parameter(description = "ID лобби") @PathVariable UUID id);

    @Operation(summary = "Добавить игрока в подходящее лобби")
    @PostMapping("/join")
    ResponseEntity<EntityModel<LobbyResponse>> joinLobby(@Valid @RequestBody JoinLobbyRequest request);

    @Operation(summary = "Принудительно распустить лобби")
    @PostMapping("/{id}/disband")
    EntityModel<LobbyResponse> disbandLobby(@PathVariable UUID id);

    @Operation(summary = "Проверить таймауты и распустить просроченные лобби")
    @PostMapping("/process-timeouts")
    List<LobbyResponse> processTimeouts();
}
