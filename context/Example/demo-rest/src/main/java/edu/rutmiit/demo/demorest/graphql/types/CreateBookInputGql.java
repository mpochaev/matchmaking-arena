package edu.rutmiit.demo.demorest.graphql.types;

/**
 * Входной тип для создания книги.
 * Соответствует input CreateBookInput в GraphQL-схеме.
 */
public record CreateBookInputGql(
        String title,
        String isbn,
        String authorId,
        String description,
        String genre,
        Integer publishedYear,
        String language
) {}
