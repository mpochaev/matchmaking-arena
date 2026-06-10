package com.example.demo.controller;

import com.example.demo.service.MatchmakingService;
import edu.rutmiit.demo.matchmakingapicontract.dto.JoinLobbyRequest;
import edu.rutmiit.demo.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.demo.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.demo.matchmakingapicontract.endpoints.LobbyApi;
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
public class LobbyController implements LobbyApi {

    private final MatchmakingService matchmakingService;

    public LobbyController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Override
    public PagedModel<EntityModel<LobbyResponse>> getAllLobbies(String status,
                                                               String region,
                                                               String rank,
                                                               String mode,
                                                               int page,
                                                               int size) {
        PagedResponse<LobbyResponse> paged = matchmakingService.findLobbies(status, region, rank, mode, page, size);
        List<EntityModel<LobbyResponse>> content = paged.content().stream()
                .map(this::toModel)
                .toList();
        return toPagedModel(content, paged, "/api/lobbies");
    }

    @Override
    public EntityModel<LobbyResponse> getLobbyById(UUID id) {
        return toModel(matchmakingService.findLobbyById(id));
    }

    @Override
    public ResponseEntity<EntityModel<LobbyResponse>> joinLobby(JoinLobbyRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toModel(matchmakingService.joinLobby(request)));
    }

    @Override
    public EntityModel<LobbyResponse> disbandLobby(UUID id) {
        return toModel(matchmakingService.disbandLobby(id));
    }

    @Override
    public List<LobbyResponse> processTimeouts() {
        return matchmakingService.processExpiredLobbies();
    }

    private EntityModel<LobbyResponse> toModel(LobbyResponse lobby) {
        EntityModel<LobbyResponse> model = EntityModel.of(lobby);
        model.add(Link.of("/api/lobbies/" + lobby.id()).withSelfRel());
        model.add(Link.of("/api/lobbies").withRel("lobbies"));
        model.add(Link.of("/api/lobbies/join").withRel("join-lobby"));
        if ("WAITING".equalsIgnoreCase(lobby.status())) {
            model.add(Link.of("/api/lobbies/" + lobby.id() + "/disband").withRel("disband"));
        }
        if (lobby.matchId() != null) {
            model.add(Link.of("/api/matches/" + lobby.matchId()).withRel("match"));
        }
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
