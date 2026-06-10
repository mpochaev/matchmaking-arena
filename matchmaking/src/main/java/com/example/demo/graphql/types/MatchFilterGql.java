package com.example.demo.graphql.types;

public record MatchFilterGql(
        String status,
        String region,
        String rank,
        String mode
) {}
