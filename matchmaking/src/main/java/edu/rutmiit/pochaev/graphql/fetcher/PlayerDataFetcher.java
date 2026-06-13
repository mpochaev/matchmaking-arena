package edu.rutmiit.pochaev.graphql.fetcher;

import edu.rutmiit.pochaev.graphql.types.CreatePlayerInputGql;
import edu.rutmiit.pochaev.graphql.types.PageInfoGql;
import edu.rutmiit.pochaev.graphql.types.PlayerConnectionGql;
import edu.rutmiit.pochaev.graphql.types.PlayerFilterGql;
import edu.rutmiit.pochaev.service.MatchmakingService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;

import java.util.UUID;

@DgsComponent
public class PlayerDataFetcher {

    private final MatchmakingService matchmakingService;

    public PlayerDataFetcher(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @DgsQuery
    public PlayerResponse player(@InputArgument String id) {
        return matchmakingService.findPlayerById(UUID.fromString(id));
    }

    @DgsQuery
    public PlayerConnectionGql players(@InputArgument PlayerFilterGql filter,
                                       @InputArgument Integer page,
                                       @InputArgument Integer size) {
        String region = filter == null ? null : filter.region();
        String rank = filter == null ? null : filter.rank();
        PagedResponse<PlayerResponse> paged = matchmakingService.findPlayers(
                region,
                rank,
                page == null ? 0 : page,
                size == null ? 20 : size
        );
        return new PlayerConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements()
        );
    }

    @DgsMutation
    public PlayerResponse createPlayer(@InputArgument CreatePlayerInputGql input) {
        return matchmakingService.createPlayer(new PlayerRequest(
                input.nickname(),
                input.rating(),
                input.region(),
                input.rank()
        ));
    }
}
