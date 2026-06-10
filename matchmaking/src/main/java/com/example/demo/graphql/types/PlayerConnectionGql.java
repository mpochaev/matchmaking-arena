package com.example.demo.graphql.types;

import edu.rutmiit.demo.matchmakingapicontract.dto.PlayerResponse;
import java.util.List;

public record PlayerConnectionGql(
        List<PlayerResponse> content,
        PageInfoGql pageInfo,
        int totalElements
) {}
