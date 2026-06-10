package com.example.demo.graphql.types;

public record JoinLobbyInputGql(
        String playerId,
        String mode,
        String region,
        String rank,
        Integer timeoutSeconds
) {}
