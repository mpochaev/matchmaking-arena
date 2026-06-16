package edu.rutmiit.pochaev.controller;

import edu.rutmiit.pochaev.assembler.MatchModelAssembler;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.endpoints.MatchApi;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.service.MatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class MatchController implements MatchApi {

    private final MatchService matchService;
    private final MatchModelAssembler matchModelAssembler;
    private final PagedResourcesAssembler<MatchResponse> pagedResourcesAssembler;

    public MatchController(MatchService matchService,
                           MatchModelAssembler matchModelAssembler,
                           PagedResourcesAssembler<MatchResponse> pagedResourcesAssembler) {
        this.matchService = matchService;
        this.matchModelAssembler = matchModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public PagedModel<EntityModel<MatchResponse>> getAllMatches(MatchStatus status,
                                                               Region region,
                                                               Rank rank,
                                                               MatchMode mode,
                                                               int page,
                                                               int size) {
        PagedResponse<MatchResponse> paged = matchService.findMatches(status, region, rank, mode, page, size);
        Page<MatchResponse> springPage = new PageImpl<>(
                paged.content(),
                PageRequest.of(paged.pageNumber(), paged.pageSize()),
                paged.totalElements()
        );
        return pagedResourcesAssembler.toModel(springPage, matchModelAssembler);
    }

    @Override
    public EntityModel<MatchResponse> getMatchById(UUID id) {
        return matchModelAssembler.toModel(matchService.findMatchById(id));
    }
}
