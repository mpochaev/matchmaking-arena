package edu.rutmiit.pochaev.graphql.types;

import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import java.util.List;

public record PlayerConnectionGql(
        List<PlayerResponse> content,
        PageInfoGql pageInfo,
        int totalElements
) {}
