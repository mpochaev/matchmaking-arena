package edu.rutmiit.demo.demorest.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import edu.rutmiit.demo.booksapicontract.dto.AuthorResponse;
import edu.rutmiit.demo.booksapicontract.dto.BookResponse;
import edu.rutmiit.demo.demorest.service.AuthorService;

/**
 * Вложенный резолвер для поля Book.author.
 *
 * В GraphQL каждое поле может иметь свой резолвер. Когда клиент запрашивает
 * книгу вместе с автором:
 *
 *   query {
 *     book(id: "1") {
 *       title
 *       author {       ← этот резолвер срабатывает
 *         fullName
 *       }
 *     }
 *   }
 *
 * DGS вызывает этот метод для каждой книги, передавая родительский объект
 * через DgsDataFetchingEnvironment. Если клиент НЕ запросил поле author,
 * этот резолвер вообще не вызывается — экономия ресурсов.
 *
 * Аннотация @DgsData(parentType, field) привязывает метод к конкретному полю
 * конкретного типа в GraphQL-схеме.
 */
@DgsComponent
public class BookAuthorDataFetcher {

    private final AuthorService authorService;

    public BookAuthorDataFetcher(AuthorService authorService) {
        this.authorService = authorService;
    }

    /**
     * Загружает автора для заданной книги.
     *
     * Родительский объект (Book) извлекается из DgsDataFetchingEnvironment.
     * В нашем in-memory хранилище автор уже вложен в BookResponse,
     * поэтому мы просто его возвращаем. В реальном проекте здесь был бы
     * вызов к базе данных или внешнему сервису.
     */
    @DgsData(parentType = "Book", field = "author")
    public AuthorResponse author(DgsDataFetchingEnvironment dfe) {
        BookResponse book = dfe.getSource();

        // Если автор уже вложен в BookResponse, возвращаем его напрямую.
        // В реальном приложении здесь мог бы быть вызов authorService.findById().
        if (book.getAuthor() != null) {
            return book.getAuthor();
        }

        // Запасной вариант — загрузить автора отдельно (для демонстрации)
        return null;
    }
}
