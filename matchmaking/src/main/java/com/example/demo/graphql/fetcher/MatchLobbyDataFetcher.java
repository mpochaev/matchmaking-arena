package com.example.demo.graphql.fetcher;

import com.example.demo.service.MatchmakingService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import edu.rutmiit.demo.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.demo.matchmakingapicontract.dto.MatchResponse;

@DgsComponent
public class MatchLobbyDataFetcher {

    private final MatchmakingService matchmakingService;

    public MatchLobbyDataFetcher(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @DgsData(parentType = "Match", field = "lobby")
    public LobbyResponse lobby(DgsDataFetchingEnvironment dfe) {
        MatchResponse match = dfe.getSource();
        return matchmakingService.findLobbyByMatchId(match.id());
    }
}
