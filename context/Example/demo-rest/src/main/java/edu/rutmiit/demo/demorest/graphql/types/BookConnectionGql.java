package edu.rutmiit.demo.demorest.graphql.types;

import edu.rutmiit.demo.booksapicontract.dto.BookResponse;

import java.util.List;

/**
 * Тип-обёртка для постраничного ответа со списком книг.
 * Соответствует типу BookConnection в GraphQL-схеме.
 */
public record BookConnectionGql(
        List<BookResponse> content,
        PageInfoGql pageInfo,
        int totalElements
) {}
