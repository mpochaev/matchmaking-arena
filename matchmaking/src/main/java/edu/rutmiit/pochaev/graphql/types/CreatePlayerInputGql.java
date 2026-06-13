package edu.rutmiit.pochaev.graphql.types;

public record CreatePlayerInputGql(
        String nickname,
        Integer rating,
        String region,
        String rank
) {}
