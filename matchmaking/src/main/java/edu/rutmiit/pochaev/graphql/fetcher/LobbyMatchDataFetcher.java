package edu.rutmiit.pochaev.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import edu.rutmiit.pochaev.service.MatchService;

@DgsComponent
public class LobbyMatchDataFetcher {

    private final MatchService matchService;

    public LobbyMatchDataFetcher(MatchService matchService) {
        this.matchService = matchService;
    }

    @DgsData(parentType = "Lobby", field = "match")
    public MatchResponse match(DgsDataFetchingEnvironment dfe) {
        LobbyResponse lobby = dfe.getSource();
        if (lobby.matchId() == null) {
            return null;
        }
        return matchService.findMatchById(lobby.matchId());
    }
}
