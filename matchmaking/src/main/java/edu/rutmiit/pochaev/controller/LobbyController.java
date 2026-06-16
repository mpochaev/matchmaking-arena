package edu.rutmiit.pochaev.controller;

import edu.rutmiit.pochaev.assembler.LobbyModelAssembler;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.JoinLobbyRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.endpoints.LobbyApi;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.service.LobbyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class LobbyController implements LobbyApi {

    private final LobbyService lobbyService;
    private final LobbyModelAssembler lobbyModelAssembler;
    private final PagedResourcesAssembler<LobbyResponse> pagedResourcesAssembler;

    public LobbyController(LobbyService lobbyService,
                           LobbyModelAssembler lobbyModelAssembler,
                           PagedResourcesAssembler<LobbyResponse> pagedResourcesAssembler) {
        this.lobbyService = lobbyService;
        this.lobbyModelAssembler = lobbyModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public PagedModel<EntityModel<LobbyResponse>> getAllLobbies(LobbyStatus status,
                                                               Region region,
                                                               Rank rank,
                                                               MatchMode mode,
                                                               int page,
                                                               int size) {
        PagedResponse<LobbyResponse> paged = lobbyService.findLobbies(status, region, rank, mode, page, size);
        Page<LobbyResponse> springPage = new PageImpl<>(
                paged.content(),
                PageRequest.of(paged.pageNumber(), paged.pageSize()),
                paged.totalElements()
        );
        return pagedResourcesAssembler.toModel(springPage, lobbyModelAssembler);
    }

    @Override
    public EntityModel<LobbyResponse> getLobbyById(UUID id) {
        return lobbyModelAssembler.toModel(lobbyService.findLobbyById(id));
    }

    @Override
    public ResponseEntity<EntityModel<LobbyResponse>> joinLobby(JoinLobbyRequest request) {
        EntityModel<LobbyResponse> model = lobbyModelAssembler.toModel(lobbyService.joinLobby(request));
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }

    @Override
    public EntityModel<LobbyResponse> leaveLobby(UUID id, UUID playerId) {
        return lobbyModelAssembler.toModel(lobbyService.leaveLobby(id, playerId));
    }

    @Override
    public EntityModel<LobbyResponse> disbandLobby(UUID id) {
        return lobbyModelAssembler.toModel(lobbyService.disbandLobby(id));
    }

    @Override
    public CollectionModel<EntityModel<LobbyResponse>> processTimeouts() {
        var lobbies = lobbyService.processExpiredLobbies().stream()
                .map(lobbyModelAssembler::toModel)
                .toList();

        return CollectionModel.of(
                lobbies,
                linkTo(methodOn(LobbyController.class).processTimeouts()).withSelfRel(),
                linkTo(methodOn(LobbyController.class).getAllLobbies(null, null, null, null, 0, 20)).withRel("collection")
        );
    }
}
