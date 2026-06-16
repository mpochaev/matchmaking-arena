package edu.rutmiit.pochaev.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.service.LobbyService;

@DgsComponent
public class MatchLobbyDataFetcher {

    private final LobbyService lobbyService;

    public MatchLobbyDataFetcher(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @DgsData(parentType = "Match", field = "lobby")
    public LobbyResponse lobby(DgsDataFetchingEnvironment dfe) {
        MatchResponse match = dfe.getSource();
        return lobbyService.findLobbyByMatchId(match.id());
    }
}
