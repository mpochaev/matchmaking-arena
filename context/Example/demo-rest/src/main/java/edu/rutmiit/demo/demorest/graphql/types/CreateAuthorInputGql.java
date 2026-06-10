package edu.rutmiit.demo.demorest.graphql.types;

import java.time.LocalDate;

/**
 * Входной тип для создания автора.
 * Соответствует input CreateAuthorInput в GraphQL-схеме.
 */
public record CreateAuthorInputGql(
        String firstName,
        String lastName,
        String email,
        String bio,
        LocalDate birthDate,
        String nationality
) {}
