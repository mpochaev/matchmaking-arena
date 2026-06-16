package edu.rutmiit.pochaev.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.service.LobbyService;

@DgsComponent
public class LobbyAverageRatingDataFetcher {

    private final LobbyService lobbyService;

    public LobbyAverageRatingDataFetcher(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @DgsData(parentType = "Lobby", field = "averageRating")
    public Double averageRating(DgsDataFetchingEnvironment dfe) {
        LobbyResponse lobby = dfe.getSource();
        return lobbyService.calculateLobbyAverageRating(lobby.id());
    }
}
