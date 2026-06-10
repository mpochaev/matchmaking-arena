package edu.rutmiit.demo.booksapicontract.dto;

import edu.rutmiit.demo.booksapicontract.validation.ValidIsbn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Запрос для частичного обновления книги (PATCH, семантика JSON Merge Patch).
 *
 * Передайте только те поля, которые нужно изменить.
 * Поля, отсутствующие в запросе, десериализуются как null — сервис их не трогает.
 *
 * Ограничение: стандартный Jackson не различает «поле не пришло» и «пришло явно null».
 * В этом контракте оба случая означают «не менять». Для точного различения
 * можно использовать JsonNullable из библиотеки jackson-databind-nullable.
 *
 * Сменить автора через PATCH нельзя — для этого создайте новую книгу.
 */
@Schema(description = "Частичное обновление книги (PATCH). Передайте только те поля, которые нужно изменить. "
        + "Непереданные поля остаются без изменений.")
public record PatchBookRequest(

        @Schema(description = "Новое название книги", example = "Война и Мир (новое издание)")
        @Size(max = 500, message = "Название не может превышать 500 символов")
        String title,

        @Schema(description = "Новый ISBN (ISBN-10 или ISBN-13)", example = "978-5-389-06259-8")
        @ValidIsbn
        String isbn,

        @Schema(description = "Новое описание книги")
        @Size(max = 5000, message = "Описание не может превышать 5000 символов")
        String description,

        @Schema(description = "Новый жанр книги", example = "Исторический роман")
        @Size(max = 100, message = "Жанр не может превышать 100 символов")
        String genre,

        @Schema(description = "Новый год публикации", example = "1869")
        @Min(value = 1, message = "Год публикации должен быть положительным")
        @Max(value = 9999, message = "Укажите корректный год публикации")
        Integer publishedYear,

        @Schema(description = "Новый язык книги (код ISO 639-1)", example = "ru")
        @Size(min = 2, max = 5, message = "Код языка должен содержать 2-5 символов")
        String language
) {}
