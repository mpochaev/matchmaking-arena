package com.example.demo.controller;

import com.example.demo.service.MatchmakingService;
import edu.rutmiit.demo.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.demo.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.demo.matchmakingapicontract.endpoints.MatchApi;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class MatchController implements MatchApi {

    private final MatchmakingService matchmakingService;

    public MatchController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Override
    public PagedModel<EntityModel<MatchResponse>> getAllMatches(String status,
                                                               String region,
                                                               String rank,
                                                               String mode,
                                                               int page,
                                                               int size) {
        PagedResponse<MatchResponse> paged = matchmakingService.findMatches(status, region, rank, mode, page, size);
        List<EntityModel<MatchResponse>> content = paged.content().stream()
                .map(this::toModel)
                .toList();
        return toPagedModel(content, paged, "/api/matches");
    }

    @Override
    public EntityModel<MatchResponse> getMatchById(UUID id) {
        return toModel(matchmakingService.findMatchById(id));
    }

    private EntityModel<MatchResponse> toModel(MatchResponse match) {
        EntityModel<MatchResponse> model = EntityModel.of(match);
        model.add(Link.of("/api/matches/" + match.id()).withSelfRel());
        model.add(Link.of("/api/matches").withRel("matches"));
        model.add(Link.of("/api/lobbies/" + match.lobbyId()).withRel("source-lobby"));
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
