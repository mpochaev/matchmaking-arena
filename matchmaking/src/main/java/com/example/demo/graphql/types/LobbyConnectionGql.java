package com.example.demo.graphql.types;

import edu.rutmiit.demo.matchmakingapicontract.dto.LobbyResponse;
import java.util.List;

public record LobbyConnectionGql(
        List<LobbyResponse> content,
        PageInfoGql pageInfo,
        int totalElements
) {}
