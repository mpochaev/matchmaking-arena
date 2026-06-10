package com.example.demo.graphql.fetcher;

import com.example.demo.service.MatchmakingService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import edu.rutmiit.demo.matchmakingapicontract.dto.LobbyResponse;

/**
 * Вычисляемое поле Lobby.averageRating.
 * В LobbyResponse такого поля нет. Оно считается только если клиент запросил его в GraphQL.
 */
@DgsComponent
public class LobbyAverageRatingDataFetcher {

    private final MatchmakingService matchmakingService;

    public LobbyAverageRatingDataFetcher(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @DgsData(parentType = "Lobby", field = "averageRating")
    public Double averageRating(DgsDataFetchingEnvironment dfe) {
        LobbyResponse lobby = dfe.getSource();
        return matchmakingService.calculateLobbyAverageRating(lobby.id());
    }
}
