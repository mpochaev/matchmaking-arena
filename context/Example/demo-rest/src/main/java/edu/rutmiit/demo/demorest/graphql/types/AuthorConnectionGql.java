package edu.rutmiit.demo.demorest.graphql.types;

import edu.rutmiit.demo.booksapicontract.dto.AuthorResponse;

import java.util.List;

/**
 * Тип-обёртка для постраничного ответа со списком авторов.
 * Соответствует типу AuthorConnection в GraphQL-схеме.
 */
public record AuthorConnectionGql(
        List<AuthorResponse> content,
        PageInfoGql pageInfo,
        int totalElements
) {}
