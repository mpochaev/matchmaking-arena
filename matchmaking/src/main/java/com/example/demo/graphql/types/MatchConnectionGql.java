package com.example.demo.graphql.types;

import edu.rutmiit.demo.matchmakingapicontract.dto.MatchResponse;
import java.util.List;

public record MatchConnectionGql(
        List<MatchResponse> content,
        PageInfoGql pageInfo,
        int totalElements
) {}
