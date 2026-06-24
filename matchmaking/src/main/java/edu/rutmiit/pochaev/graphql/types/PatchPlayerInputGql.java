package edu.rutmiit.pochaev.graphql.types;

import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;

public record PatchPlayerInputGql(
        String nickname,
        Integer rating,
        Region region
) {}
