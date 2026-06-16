package edu.rutmiit.pochaev.graphql.types;

import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.MatchMode;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Rank;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.Region;

public record LobbyFilterGql(
        LobbyStatus status,
        Region region,
        Rank rank,
        MatchMode mode
) {}
