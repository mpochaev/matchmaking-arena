package edu.rutmiit.pochaev.matchmakingapicontract.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PagedResponse<T> of(List<T> allItems, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 20;
        int from = Math.min(safePage * safeSize, allItems.size());
        int to = Math.min(from + safeSize, allItems.size());
        List<T> content = allItems.subList(from, to);
        int totalPages = allItems.isEmpty() ? 0 : (int) Math.ceil((double) allItems.size() / safeSize);
        boolean last = totalPages == 0 || safePage >= totalPages - 1;
        return new PagedResponse<>(content, safePage, safeSize, allItems.size(), totalPages, last);
    }
}
