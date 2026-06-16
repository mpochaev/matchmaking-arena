package edu.rutmiit.pochaev.graphql.types;

import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;

public record JoinLobbyInputGql(
        String playerId,
        MatchMode mode,
        Region region,
        Integer timeoutSeconds
) {}
