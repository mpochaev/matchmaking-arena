package edu.rutmiit.demo.demorest.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.demo.booksapicontract.dto.BookRequest;
import edu.rutmiit.demo.booksapicontract.dto.BookResponse;
import edu.rutmiit.demo.booksapicontract.dto.PagedResponse;
import edu.rutmiit.demo.booksapicontract.dto.UpdateBookRequest;
import edu.rutmiit.demo.demorest.graphql.types.BookConnectionGql;
import edu.rutmiit.demo.demorest.graphql.types.BookFilterGql;
import edu.rutmiit.demo.demorest.graphql.types.CreateBookInputGql;
import edu.rutmiit.demo.demorest.graphql.types.PageInfoGql;
import edu.rutmiit.demo.demorest.graphql.types.UpdateBookInputGql;
import edu.rutmiit.demo.demorest.service.BookService;

/**
 * DataFetcher для операций с книгами.
 *
 * Аннотация @DgsComponent регистрирует этот класс как компонент DGS-фреймворка.
 * Каждый метод с @DgsQuery или @DgsMutation привязывается к соответствующему полю
 * в GraphQL-схеме. DGS находит их по имени метода (или по явному параметру field).
 *
 * Этот DataFetcher обрабатывает корневые поля Query и Mutation для книг.
 * Вложенные поля (Book.author) обрабатываются в отдельном резолвере.
 */
@DgsComponent
public class BookDataFetcher {

    private final BookService bookService;

    public BookDataFetcher(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Получение книги по идентификатору.
     * Соответствует полю Query.book(id: ID!) в схеме.
     * Возвращает null если книга не найдена (вместо исключения, как принято в GraphQL).
     */
    @DgsQuery
    public BookResponse book(@InputArgument String id) {
        return bookService.findBookById(Long.parseLong(id));
    }

    /**
     * Список книг с фильтрацией и пагинацией.
     * Соответствует полю Query.books(filter, page, size) в схеме.
     *
     * @InputArgument автоматически маппит GraphQL-аргументы на Java-параметры.
     * Для сложных типов (input BookFilter) DGS сам десериализует JSON в объект.
     */
    @DgsQuery
    public BookConnectionGql books(
            @InputArgument BookFilterGql filter,
            @InputArgument Integer page,
            @InputArgument Integer size) {

        // Подставляем значения по умолчанию, если клиент не передал аргументы
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        // Извлекаем параметры фильтрации
        Long authorId = null;
        String genre = null;
        Integer publishedYear = null;
        String titleSearch = null;

        if (filter != null) {
            authorId = filter.authorId() != null ? Long.parseLong(filter.authorId()) : null;
            genre = filter.genre();
            publishedYear = filter.publishedYear();
            titleSearch = filter.titleSearch();
        }

        // Переиспользуем существующий сервисный слой — GraphQL не дублирует бизнес-логику
        PagedResponse<BookResponse> paged = bookService.findAllBooks(
                authorId, genre, publishedYear, titleSearch, pageNum, pageSize);

        return new BookConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements());
    }

    /**
     * Создание книги.
     * Соответствует полю Mutation.createBook(input: CreateBookInput!) в схеме.
     */
    @DgsMutation
    public BookResponse createBook(@InputArgument CreateBookInputGql input) {
        BookRequest request = new BookRequest(
                input.title(),
                input.isbn(),
                Long.parseLong(input.authorId()),
                input.description(),
                input.genre(),
                input.publishedYear(),
                input.language()
        );
        return bookService.createBook(request);
    }

    /**
     * Обновление книги.
     * Соответствует полю Mutation.updateBook(id, input) в схеме.
     */
    @DgsMutation
    public BookResponse updateBook(@InputArgument String id, @InputArgument UpdateBookInputGql input) {
        UpdateBookRequest request = new UpdateBookRequest(
                input.title(),
                input.isbn(),
                input.description(),
                input.genre(),
                input.publishedYear(),
                input.language()
        );
        return bookService.updateBook(Long.parseLong(id), request);
    }

    /**
     * Удаление книги.
     * Соответствует полю Mutation.deleteBook(id) в схеме.
     * Возвращает true при успешном удалении.
     */
    @DgsMutation
    public boolean deleteBook(@InputArgument String id) {
        bookService.deleteBook(Long.parseLong(id));
        return true;
    }
}
