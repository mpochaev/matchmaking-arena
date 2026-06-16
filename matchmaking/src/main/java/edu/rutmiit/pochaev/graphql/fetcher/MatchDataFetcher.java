package edu.rutmiit.pochaev.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.pochaev.graphql.types.MatchConnectionGql;
import edu.rutmiit.pochaev.graphql.types.MatchFilterGql;
import edu.rutmiit.pochaev.graphql.types.PageInfoGql;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.service.MatchService;

import java.util.UUID;

@DgsComponent
public class MatchDataFetcher {

    private final MatchService matchService;

    public MatchDataFetcher(MatchService matchService) {
        this.matchService = matchService;
    }

    @DgsQuery
    public MatchResponse match(@InputArgument String id) {
        return matchService.findMatchById(UUID.fromString(id));
    }

    @DgsQuery
    public MatchConnectionGql matches(@InputArgument MatchFilterGql filter,
                                      @InputArgument Integer page,
                                      @InputArgument Integer size) {
        MatchStatus status = filter == null ? null : filter.status();
        Region region = filter == null ? null : filter.region();
        Rank rank = filter == null ? null : filter.rank();
        MatchMode mode = filter == null ? null : filter.mode();
        PagedResponse<MatchResponse> paged = matchService.findMatches(
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
