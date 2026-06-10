package com.example.demo.graphql.fetcher;

import com.example.demo.graphql.types.JoinLobbyInputGql;
import com.example.demo.graphql.types.LobbyConnectionGql;
import com.example.demo.graphql.types.LobbyFilterGql;
import com.example.demo.graphql.types.PageInfoGql;
import com.example.demo.service.MatchmakingService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.demo.matchmakingapicontract.dto.JoinLobbyRequest;
import edu.rutmiit.demo.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.demo.matchmakingapicontract.dto.PagedResponse;

import java.util.List;
import java.util.UUID;

@DgsComponent
public class LobbyDataFetcher {

    private final MatchmakingService matchmakingService;

    public LobbyDataFetcher(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @DgsQuery
    public LobbyResponse lobby(@InputArgument String id) {
        return matchmakingService.findLobbyById(UUID.fromString(id));
    }

    @DgsQuery
    public LobbyConnectionGql lobbies(@InputArgument LobbyFilterGql filter,
                                      @InputArgument Integer page,
                                      @InputArgument Integer size) {
        String status = filter == null ? null : filter.status();
        String region = filter == null ? null : filter.region();
        String rank = filter == null ? null : filter.rank();
        String mode = filter == null ? null : filter.mode();
        PagedResponse<LobbyResponse> paged = matchmakingService.findLobbies(
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
    public LobbyConnectionGql lobbiesByRegion(@InputArgument String region,
                                              @InputArgument Integer page,
                                              @InputArgument Integer size) {
        PagedResponse<LobbyResponse> paged = matchmakingService.findLobbiesByRegion(
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
        return matchmakingService.joinLobby(new JoinLobbyRequest(
                UUID.fromString(input.playerId()),
                input.mode(),
                input.region(),
                input.rank(),
                input.timeoutSeconds()
        ));
    }

    @DgsMutation
    public LobbyResponse disbandLobby(@InputArgument String id) {
        return matchmakingService.disbandLobby(UUID.fromString(id));
    }

    @DgsMutation
    public List<LobbyResponse> processLobbyTimeouts() {
        return matchmakingService.processExpiredLobbies();
    }
}
