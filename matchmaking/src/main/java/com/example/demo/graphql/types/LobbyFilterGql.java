package com.example.demo.graphql.types;

public record LobbyFilterGql(
        String status,
        String region,
        String rank,
        String mode
) {}
