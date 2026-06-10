package edu.rutmiit.demo.demorest.graphql.types;

/**
 * Входной тип для обновления книги.
 * Соответствует input UpdateBookInput в GraphQL-схеме.
 * Автора изменить нельзя.
 */
public record UpdateBookInputGql(
        String title,
        String isbn,
        String description,
        String genre,
        Integer publishedYear,
        String language
) {}
