package edu.rutmiit.pochaev.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.pochaev.graphql.types.CreatePlayerInputGql;
import edu.rutmiit.pochaev.graphql.types.PageInfoGql;
import edu.rutmiit.pochaev.graphql.types.PatchPlayerInputGql;
import edu.rutmiit.pochaev.graphql.types.PlayerConnectionGql;
import edu.rutmiit.pochaev.graphql.types.PlayerFilterGql;
import edu.rutmiit.pochaev.graphql.types.UpdatePlayerInputGql;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PatchPlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.UpdatePlayerRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.service.PlayerService;

import java.util.UUID;

@DgsComponent
public class PlayerDataFetcher {

    private final PlayerService playerService;

    public PlayerDataFetcher(PlayerService playerService) {
        this.playerService = playerService;
    }

    @DgsQuery
    public PlayerResponse player(@InputArgument String id) {
        return playerService.findPlayerById(UUID.fromString(id));
    }

    @DgsQuery
    public PlayerConnectionGql players(@InputArgument PlayerFilterGql filter,
                                       @InputArgument Integer page,
                                       @InputArgument Integer size) {
        Region region = filter == null ? null : filter.region();
        Rank rank = filter == null ? null : filter.rank();
        PagedResponse<PlayerResponse> paged = playerService.findPlayers(
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
        return playerService.createPlayer(new PlayerRequest(
                input.nickname(),
                input.rating(),
                input.region()
        ));
    }

    @DgsMutation
    public PlayerResponse updatePlayer(@InputArgument String id, @InputArgument UpdatePlayerInputGql input) {
        return playerService.updatePlayer(UUID.fromString(id), new UpdatePlayerRequest(
                input.nickname(),
                input.rating(),
                input.region()
        ));
    }

    @DgsMutation
    public PlayerResponse patchPlayer(@InputArgument String id, @InputArgument PatchPlayerInputGql input) {
        return playerService.patchPlayer(UUID.fromString(id), new PatchPlayerRequest(
                input.nickname(),
                input.rating(),
                input.region()
        ));
    }
}
