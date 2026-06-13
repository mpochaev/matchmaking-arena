package edu.rutmiit.pochaev.graphql.fetcher;

import edu.rutmiit.pochaev.graphql.types.MatchConnectionGql;
import edu.rutmiit.pochaev.graphql.types.MatchFilterGql;
import edu.rutmiit.pochaev.graphql.types.PageInfoGql;
import edu.rutmiit.pochaev.service.MatchmakingService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;

import java.util.UUID;

@DgsComponent
public class MatchDataFetcher {

    private final MatchmakingService matchmakingService;

    public MatchDataFetcher(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @DgsQuery
    public MatchResponse match(@InputArgument String id) {
        return matchmakingService.findMatchById(UUID.fromString(id));
    }

    @DgsQuery
    public MatchConnectionGql matches(@InputArgument MatchFilterGql filter,
                                      @InputArgument Integer page,
                                      @InputArgument Integer size) {
        String status = filter == null ? null : filter.status();
        String region = filter == null ? null : filter.region();
        String rank = filter == null ? null : filter.rank();
        String mode = filter == null ? null : filter.mode();
        PagedResponse<MatchResponse> paged = matchmakingService.findMatches(
                status,
                region,
                rank,
                mode,
                page == null ? 0 : page,
                size == null ? 20 : size
        );
        return new MatchConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements()
        );
    }
}
