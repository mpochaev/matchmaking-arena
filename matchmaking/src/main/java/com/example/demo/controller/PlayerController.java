package com.example.demo.controller;

import com.example.demo.service.MatchmakingService;
import edu.rutmiit.demo.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.demo.matchmakingapicontract.dto.PlayerRequest;
import edu.rutmiit.demo.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.demo.matchmakingapicontract.endpoints.PlayerApi;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class PlayerController implements PlayerApi {

    private final MatchmakingService matchmakingService;

    public PlayerController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Override
    public PagedModel<EntityModel<PlayerResponse>> getAllPlayers(String region, String rank, int page, int size) {
        PagedResponse<PlayerResponse> paged = matchmakingService.findPlayers(region, rank, page, size);
        List<EntityModel<PlayerResponse>> content = paged.content().stream()
                .map(this::toModel)
                .toList();
        return toPagedModel(content, paged, "/api/players");
    }

    @Override
    public EntityModel<PlayerResponse> getPlayerById(UUID id) {
        return toModel(matchmakingService.findPlayerById(id));
    }

    @Override
    public ResponseEntity<EntityModel<PlayerResponse>> createPlayer(PlayerRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toModel(matchmakingService.createPlayer(request)));
    }

    private EntityModel<PlayerResponse> toModel(PlayerResponse player) {
        EntityModel<PlayerResponse> model = EntityModel.of(player);
        model.add(Link.of("/api/players/" + player.id()).withSelfRel());
        model.add(Link.of("/api/players").withRel("players"));
        model.add(Link.of("/api/lobbies/join").withRel("join-lobby"));
        return model;
    }

    private <T> PagedModel<EntityModel<T>> toPagedModel(List<EntityModel<T>> content,
                                                       PagedResponse<?> paged,
                                                       String basePath) {
        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                paged.pageSize(), paged.pageNumber(), paged.totalElements(), paged.totalPages());

        List<Link> links = new ArrayList<>();
        links.add(Link.of(basePath + "?page=" + paged.pageNumber() + "&size=" + paged.pageSize()).withSelfRel());
        if (paged.pageNumber() > 0) {
            links.add(Link.of(basePath + "?page=" + (paged.pageNumber() - 1) + "&size=" + paged.pageSize()).withRel("prev"));
        }
        if (!paged.last()) {
            links.add(Link.of(basePath + "?page=" + (paged.pageNumber() + 1) + "&size=" + paged.pageSize()).withRel("next"));
        }
        return PagedModel.of(content, metadata, links);
    }
}
