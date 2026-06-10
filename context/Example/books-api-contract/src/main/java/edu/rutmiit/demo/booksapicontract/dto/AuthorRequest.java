package edu.rutmiit.demo.booksapicontract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * DTO для создания или полного обновления автора (POST / PUT).
 * Все обязательные поля должны присутствовать.
 */
@Schema(description = "Запрос на создание или полное обновление автора")
public record AuthorRequest(

        @Schema(description = "Имя автора", example = "Лев", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Имя автора не может быть пустым")
        @Size(max = 100, message = "Имя не может превышать 100 символов")
        String firstName,

        @Schema(description = "Фамилия автора", example = "Толстой", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Фамилия автора не может быть пустой")
        @Size(max = 100, message = "Фамилия не может превышать 100 символов")
        String lastName,

        @Schema(description = "Email автора", example = "tolstoy@example.com")
        @Email(message = "Некорректный формат email")
        @Size(max = 255, message = "Email не может превышать 255 символов")
        String email,

        @Schema(description = "Краткая биография автора", example = "Русский писатель, один из величайших в мировой литературе.")
        @Size(max = 2000, message = "Биография не может превышать 2000 символов")
        String bio,

        @Schema(description = "Дата рождения автора", example = "1828-09-09")
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        @Schema(description = "Национальность автора", example = "Русский")
        @Size(max = 100, message = "Национальность не может превышать 100 символов")
        String nationality
) {}