package edu.rutmiit.pochaev.graphql.types;

public record LobbyFilterGql(
        String status,
        String region,
        String rank,
        String mode
) {}
