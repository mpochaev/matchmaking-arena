package edu.rutmiit.demo.demorest.graphql.types;

import java.time.LocalDate;

/**
 * Входной тип для обновления автора.
 * Соответствует input UpdateAuthorInput в GraphQL-схеме.
 */
public record UpdateAuthorInputGql(
        String firstName,
        String lastName,
        String email,
        String bio,
        LocalDate birthDate,
        String nationality
) {}
