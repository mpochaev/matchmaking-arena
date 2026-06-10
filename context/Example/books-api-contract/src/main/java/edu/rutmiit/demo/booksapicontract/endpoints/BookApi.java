package edu.rutmiit.demo.booksapicontract.endpoints;

import edu.rutmiit.demo.booksapicontract.config.BooksApiContractConfig;
import edu.rutmiit.demo.booksapicontract.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контракт API для управления книгами.
 * Реализующий контроллер в сервисе должен имплементировать этот интерфейс.
 */
@Tag(name = "Books", description = "Управление книгами в каталоге")
@RequestMapping(
        value = "/api/books",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public interface BookApi {

    @Operation(
            summary = "Получить книгу по ID",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Книга найдена")
    @ApiResponse(responseCode = "404", description = "Книга не найдена",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    EntityModel<BookResponse> getBookById(
            @Parameter(description = "ID книги", required = true, example = "1") @PathVariable Long id
    );

    @Operation(
            summary = "Список книг",
            description = """
                    Возвращает постраничный список книг с HATEOAS-ссылками.
                    Поддерживает комбинирование фильтров: authorId, genre, publishedYear и titleSearch
                    можно передавать одновременно.
                    """,
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Постраничный список книг")
    @GetMapping
    PagedModel<EntityModel<BookResponse>> getAllBooks(
            @Parameter(description = "Фильтр по ID автора") @RequestParam(required = false) Long authorId,
            @Parameter(description = "Фильтр по жанру", example = "Роман") @RequestParam(required = false) String genre,
            @Parameter(description = "Фильтр по году публикации", example = "1869") @RequestParam(required = false) Integer publishedYear,
            @Parameter(description = "Поиск по названию (substring, case-insensitive)", example = "Война") @RequestParam(required = false) String titleSearch,
            @Parameter(description = "Номер страницы (0..N)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20") @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "Создать книгу",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "201", description = "Книга создана. Location header содержит URI нового ресурса.")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Автор с указанным authorId не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Книга с таким ISBN уже существует",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<BookResponse>> createBook(@Valid @RequestBody BookRequest request);

    @Operation(
            summary = "Полное обновление книги (PUT)",
            description = "Заменяет все поля книги. Автора изменить нельзя. "
                    + "Для обновления отдельных полей используйте PATCH.",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Книга обновлена")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Книга не найдена",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Книга с таким ISBN уже существует",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<BookResponse> updateBook(
            @Parameter(description = "ID книги", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateBookRequest request
    );

    @Operation(
            summary = "Частичное обновление книги (PATCH)",
            description = """
                    Обновляет только переданные поля (семантика JSON Merge Patch, RFC 7396).
                    Непереданные поля остаются без изменений. Автора книги изменить нельзя.
                    """,
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Книга обновлена")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Книга не найдена",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Книга с таким ISBN уже существует",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<BookResponse> patchBook(
            @Parameter(description = "ID книги", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody PatchBookRequest request
    );

    @Operation(
            summary = "Удалить книгу",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "204", description = "Книга удалена")
    @ApiResponse(responseCode = "404", description = "Книга не найдена",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBook(
            @Parameter(description = "ID книги", required = true, example = "1") @PathVariable Long id
    );
}

  