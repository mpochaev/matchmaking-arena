package edu.rutmiit.pochaev.graphql.types;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import java.util.List;

public record LobbyConnectionGql(
        List<LobbyResponse> content,
        PageInfoGql pageInfo,
        int totalElements
) {}
