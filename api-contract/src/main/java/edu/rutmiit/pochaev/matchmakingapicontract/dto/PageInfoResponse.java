package edu.rutmiit.pochaev.matchmakingapicontract.dto;

public record PageInfoResponse(
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {}
