package edu.rutmiit.pochaev.graphql.fetcher;

import edu.rutmiit.pochaev.service.MatchmakingService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;

@DgsComponent
public class LobbyMatchDataFetcher {

    private final MatchmakingService matchmakingService;

    public LobbyMatchDataFetcher(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @DgsData(parentType = "Lobby", field = "match")
    public MatchResponse match(DgsDataFetchingEnvironment dfe) {
        LobbyResponse lobby = dfe.getSource();
        if (lobby.matchId() == null) {
            return null;
        }
        return matchmakingService.findMatchById(lobby.matchId());
    }
}
