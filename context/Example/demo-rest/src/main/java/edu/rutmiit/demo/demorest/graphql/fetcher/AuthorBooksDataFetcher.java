package edu.rutmiit.demo.demorest.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.demo.booksapicontract.dto.AuthorResponse;
import edu.rutmiit.demo.booksapicontract.dto.BookResponse;
import edu.rutmiit.demo.booksapicontract.dto.PagedResponse;
import edu.rutmiit.demo.demorest.graphql.types.BookConnectionGql;
import edu.rutmiit.demo.demorest.graphql.types.PageInfoGql;
import edu.rutmiit.demo.demorest.service.BookService;

/**
 * Вложенный резолвер для поля Author.books.
 *
 * Срабатывает когда клиент запрашивает книги автора:
 *
 *   query {
 *     author(id: "1") {
 *       fullName
 *       books(page: 0, size: 5) {    ← этот резолвер
 *         content {
 *           title
 *         }
 *         totalElements
 *       }
 *     }
 *   }
 *
 * Демонстрирует работу с аргументами вложенного поля (page, size)
 * и доступ к родительскому объекту (Author).
 */
@DgsComponent
public class AuthorBooksDataFetcher {

    private final BookService bookService;

    public AuthorBooksDataFetcher(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Загружает книги указанного автора с пагинацией.
     *
     * Аргументы (page, size) берутся из GraphQL-запроса через @InputArgument.
     * Родительский объект (Author) берётся из DgsDataFetchingEnvironment.
     */
    @DgsData(parentType = "Author", field = "books")
    public BookConnectionGql books(
            DgsDataFetchingEnvironment dfe,
            @InputArgument Integer page,
            @InputArgument Integer size) {

        AuthorResponse author = dfe.getSource();

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        // Фильтруем книги по ID автора — переиспользуем сервис
        PagedResponse<BookResponse> paged = bookService.findAllBooks(
                author.getId(), null, null, null, pageNum, pageSize);

        return new BookConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements());
    }
}
