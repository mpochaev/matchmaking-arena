package edu.rutmiit.pochaev.graphql.types;

public record JoinLobbyInputGql(
        String playerId,
        String mode,
        String region,
        String rank,
        Integer timeoutSeconds
) {}
