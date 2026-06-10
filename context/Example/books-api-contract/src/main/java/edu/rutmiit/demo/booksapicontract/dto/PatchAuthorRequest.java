package edu.rutmiit.demo.booksapicontract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Запрос для частичного обновления автора (PATCH, семантика JSON Merge Patch).
 *
 * Все поля необязательны. Передайте только то, что нужно изменить.
 * Поля, которые не переданы (null), сервис оставляет без изменений.
 */
@Schema(description = "Частичное обновление автора (PATCH). Передайте только те поля, которые нужно изменить.")
public record PatchAuthorRequest(

        @Schema(description = "Новое имя автора", example = "Алексей")
        @Size(max = 100, message = "Имя не может превышать 100 символов")
        String firstName,

        @Schema(description = "Новая фамилия автора", example = "Толстой")
        @Size(max = 100, message = "Фамилия не может превышать 100 символов")
        String lastName,

        @Schema(description = "Новый email автора", example = "tolstoy@example.com")
        @Email(message = "Некорректный формат email")
        @Size(max = 255, message = "Email не может превышать 255 символов")
        String email,

        @Schema(description = "Новая биография автора")
        @Size(max = 2000, message = "Биография не может превышать 2000 символов")
        String bio,

        @Schema(description = "Новая дата рождения автора", example = "1828-09-09")
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        @Schema(description = "Новая национальность автора", example = "Русский")
        @Size(max = 100, message = "Национальность не может превышать 100 символов")
        String nationality
) {}
