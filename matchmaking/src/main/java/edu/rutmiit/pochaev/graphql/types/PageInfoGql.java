package edu.rutmiit.pochaev.graphql.types;

public record PageInfoGql(
        int pageNumber,
        int pageSize,
        int totalPages,
        boolean last
) {}
