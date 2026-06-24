package edu.rutmiit.pochaev.controller;

import edu.rutmiit.pochaev.assembler.PlayerModelAssembler;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PatchPlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.UpdatePlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.endpoints.PlayerApi;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.service.PlayerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PlayerController implements PlayerApi {

    private final PlayerService playerService;
    private final PlayerModelAssembler playerModelAssembler;
    private final PagedResourcesAssembler<PlayerResponse> pagedResourcesAssembler;

    public PlayerController(PlayerService playerService,
                            PlayerModelAssembler playerModelAssembler,
                            PagedResourcesAssembler<PlayerResponse> pagedResourcesAssembler) {
        this.playerService = playerService;
        this.playerModelAssembler = playerModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public PagedModel<EntityModel<PlayerResponse>> getAllPlayers(Region region, Rank rank, int page, int size) {
        PagedResponse<PlayerResponse> paged = playerService.findPlayers(region, rank, page, size);
        Page<PlayerResponse> springPage = new PageImpl<>(
                paged.content(),
                PageRequest.of(paged.pageNumber(), paged.pageSize()),
                paged.totalElements()
        );
        return pagedResourcesAssembler.toModel(springPage, playerModelAssembler);
    }

    @Override
    public EntityModel<PlayerResponse> getPlayerById(UUID id) {
        return playerModelAssembler.toModel(playerService.findPlayerById(id));
    }

    @Override
    public ResponseEntity<EntityModel<PlayerResponse>> createPlayer(PlayerRequest request) {
        EntityModel<PlayerResponse> model = playerModelAssembler.toModel(playerService.createPlayer(request));
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }


    @Override
    public EntityModel<PlayerResponse> updatePlayer(UUID id, UpdatePlayerRequest request) {
        return playerModelAssembler.toModel(playerService.updatePlayer(id, request));
    }

    @Override
    public EntityModel<PlayerResponse> patchPlayer(UUID id, PatchPlayerRequest request) {
        return playerModelAssembler.toModel(playerService.patchPlayer(id, request));
    }

    @Override
    public ResponseEntity<Void> deletePlayer(UUID id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
