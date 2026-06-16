package edu.rutmiit.pochaev.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.pochaev.graphql.types.JoinLobbyInputGql;
import edu.rutmiit.pochaev.graphql.types.LobbyConnectionGql;
import edu.rutmiit.pochaev.graphql.types.LobbyFilterGql;
import edu.rutmiit.pochaev.graphql.types.PageInfoGql;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.JoinLobbyRequest;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PagedResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;
import edu.rutmiit.pochaev.service.LobbyService;

import java.util.List;
import java.util.UUID;

@DgsComponent
public class LobbyDataFetcher {

    private final LobbyService lobbyService;

    public LobbyDataFetcher(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @DgsQuery
    public LobbyResponse lobby(@InputArgument String id) {
        return lobbyService.findLobbyById(UUID.fromString(id));
    }

    @DgsQuery
    public LobbyConnectionGql lobbies(@InputArgument LobbyFilterGql filter,
                                      @InputArgument Integer page,
                                      @InputArgument Integer size) {
        LobbyStatus status = filter == null ? null : filter.status();
        Region region = filter == null ? null : filter.region();
        Rank rank = filter == null ? null : filter.rank();
        MatchMode mode = filter == null ? null : filter.mode();
        PagedResponse<LobbyResponse> paged = lobbyService.findLobbies(
                status,
                region,
                rank,
                mode,
                page == null ? 0 : page,
                size == null ? 20 : size
        );
        return new LobbyConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements()
        );
    }

    @DgsQuery
    public LobbyConnectionGql lobbiesByRegion(@InputArgument Region region,
                                              @InputArgument Integer page,
                                              @InputArgument Integer size) {
        PagedResponse<LobbyResponse> paged = lobbyService.findLobbiesByRegion(
                region,
                page == null ? 0 : page,
                size == null ? 20 : size
        );
        return new LobbyConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements()
        );
    }

    @DgsMutation
    public LobbyResponse joinLobby(@InputArgument JoinLobbyInputGql input) {
        return lobbyService.joinLobby(new JoinLobbyRequest(
                UUID.fromString(input.playerId()),
                input.mode(),
                input.region(),
                input.timeoutSeconds()
        ));
    }

    @DgsMutation
    public LobbyResponse leaveLobby(@InputArgument String id, @InputArgument String playerId) {
        return lobbyService.leaveLobby(UUID.fromString(id), UUID.fromString(playerId));
    }

    @DgsMutation
    public LobbyResponse disbandLobby(@InputArgument String id) {
        return lobbyService.disbandLobby(UUID.fromString(id));
    }

    @DgsMutation
    public List<LobbyResponse> processLobbyTimeouts() {
        return lobbyService.processExpiredLobbies();
    }
}
