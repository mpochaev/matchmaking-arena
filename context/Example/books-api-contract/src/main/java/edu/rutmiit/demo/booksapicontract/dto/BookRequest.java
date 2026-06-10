package edu.rutmiit.demo.booksapicontract.dto;

import edu.rutmiit.demo.booksapicontract.validation.ValidIsbn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * DTO для создания новой книги.
 */
@Schema(description = "Запрос на создание книги")
public record BookRequest(

        @Schema(description = "Название книги", example = "Война и мир", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Название не может быть пустым")
        @Size(max = 500, message = "Название не может превышать 500 символов")
        String title,

        @Schema(description = "ISBN книги (ISBN-10 или ISBN-13)", example = "978-5-389-06259-8", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "ISBN не может быть пустым")
        @ValidIsbn
        String isbn,

        @Schema(description = "ID автора книги", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "ID автора не может быть пустым")
        Long authorId,

        @Schema(description = "Краткое описание книги", example = "Эпический роман о событиях войны 1812 года.")
        @Size(max = 5000, message = "Описание не может превышать 5000 символов")
        String description,

        @Schema(description = "Жанр книги", example = "Исторический роман")
        @Size(max = 100, message = "Жанр не может превышать 100 символов")
        String genre,

        @Schema(description = "Год первой публикации", example = "1869")
        @Min(value = 1, message = "Год публикации должен быть положительным")
        @Max(value = 9999, message = "Укажите корректный год публикации")
        Integer publishedYear,

        @Schema(description = "Язык книги (код ISO 639-1)", example = "ru")
        @Size(min = 2, max = 5, message = "Код языка должен содержать 2-5 символов")
        String language
) {}