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
 * Контракт API для управления авторами.
 * Реализующий контроллер в сервисе должен имплементировать этот интерфейс.
 */
@Tag(name = "Authors", description = "Управление авторами книжного каталога")
@RequestMapping(
        value = "/api/authors",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public interface AuthorApi {

    @Operation(
            summary = "Список авторов",
            description = "Возвращает постраничный список авторов с HATEOAS-ссылками. "
                    + "Ссылки prev/next позволяют клиенту навигировать по страницам без знания офсетов.",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Список авторов")
    @GetMapping
    PagedModel<EntityModel<AuthorResponse>> getAllAuthors(
            @Parameter(description = "Номер страницы (0..N)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "Получить автора по ID",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Автор найден")
    @ApiResponse(responseCode = "404", description = "Автор не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    EntityModel<AuthorResponse> getAuthorById(
            @Parameter(description = "ID автора", required = true, example = "1") @PathVariable Long id
    );

    @Operation(
            summary = "Создать автора",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "201", description = "Автор создан. Location header содержит URI нового ресурса.")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<AuthorResponse>> createAuthor(@Valid @RequestBody AuthorRequest request);

    @Operation(
            summary = "Полное обновление автора (PUT)",
            description = "Заменяет все поля автора. Для обновления отдельных полей используйте PATCH.",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Автор обновлён")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Автор не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<AuthorResponse> updateAuthor(
            @Parameter(description = "ID автора", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody AuthorRequest request
    );

    @Operation(
            summary = "Частичное обновление автора (PATCH)",
            description = """
                    Обновляет только переданные поля (семантика JSON Merge Patch, RFC 7396).
                    Непереданные поля остаются без изменений.
                    """,
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Автор обновлён")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Автор не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<AuthorResponse> patchAuthor(
            @Parameter(description = "ID автора", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody PatchAuthorRequest request
    );

    @Operation(
            summary = "Удалить автора",
            description = "Удаляет автора и все его книги (каскадное удаление).",
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "204", description = "Автор удалён")
    @ApiResponse(responseCode = "404", description = "Автор не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAuthor(
            @Parameter(description = "ID автора", required = true, example = "1") @PathVariable Long id
    );

    @Operation(
            summary = "Книги автора (суб-ресурс)",
            description = """
                    Возвращает постраничный список книг указанного автора.
                    Это суб-ресурс (концепция REST): /authors/{id}/books.
                    Эквивалентен GET /books?authorId={id}, но точнее отражает иерархию.
                    """,
            security = @SecurityRequirement(name = BooksApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Список книг автора")
    @ApiResponse(responseCode = "404", description = "Автор не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}/books")
    PagedModel<EntityModel<BookResponse>> getBooksByAuthor(
            @Parameter(description = "ID автора", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Номер страницы (0..N)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20") @RequestParam(defaultValue = "20") int size
    );
}
