package edu.rutmiit.pochaev.graphql.types;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import java.util.List;

public record MatchConnectionGql(
        List<MatchResponse> content,
        PageInfoGql pageInfo,
        int totalElements
) {}
