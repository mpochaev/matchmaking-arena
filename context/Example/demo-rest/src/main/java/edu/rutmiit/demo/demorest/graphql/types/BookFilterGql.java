package edu.rutmiit.demo.demorest.graphql.types;

/**
 * Входной тип для фильтрации книг.
 * Соответствует input BookFilter в GraphQL-схеме.
 *
 * Все поля необязательны — клиент передаёт только нужные фильтры.
 */
public record BookFilterGql(
        String authorId,
        String genre,
        Integer publishedYear,
        String titleSearch
) {}
