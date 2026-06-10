package edu.rutmiit.demo.demorest.graphql.fetcher;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import edu.rutmiit.demo.booksapicontract.dto.AuthorRequest;
import edu.rutmiit.demo.booksapicontract.dto.AuthorResponse;
import edu.rutmiit.demo.booksapicontract.dto.PagedResponse;
import edu.rutmiit.demo.demorest.graphql.types.AuthorConnectionGql;
import edu.rutmiit.demo.demorest.graphql.types.CreateAuthorInputGql;
import edu.rutmiit.demo.demorest.graphql.types.PageInfoGql;
import edu.rutmiit.demo.demorest.graphql.types.UpdateAuthorInputGql;
import edu.rutmiit.demo.demorest.service.AuthorService;

/**
 * DataFetcher для операций с авторами.
 *
 * Обрабатывает корневые поля Query и Mutation, связанные с авторами.
 * Вложенные поля (Author.books) обрабатываются в AuthorBooksDataFetcher.
 *
 * Принцип разделения: один DataFetcher — одна группа связанных операций.
 * Это делает код более читаемым и тестируемым.
 */
@DgsComponent
public class AuthorDataFetcher {

    private final AuthorService authorService;

    public AuthorDataFetcher(AuthorService authorService) {
        this.authorService = authorService;
    }

    /**
     * Получение автора по идентификатору.
     * Соответствует полю Query.author(id: ID!) в схеме.
     */
    @DgsQuery
    public AuthorResponse author(@InputArgument String id) {
        return authorService.findById(Long.parseLong(id));
    }

    /**
     * Список авторов с пагинацией.
     * Соответствует полю Query.authors(page, size) в схеме.
     */
    @DgsQuery
    public AuthorConnectionGql authors(
            @InputArgument Integer page,
            @InputArgument Integer size) {

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        PagedResponse<AuthorResponse> paged = authorService.findAll(pageNum, pageSize);

        return new AuthorConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements());
    }

    /**
     * Создание автора.
     * Соответствует полю Mutation.createAuthor(input) в схеме.
     */
    @DgsMutation
    public AuthorResponse createAuthor(@InputArgument CreateAuthorInputGql input) {
        AuthorRequest request = new AuthorRequest(
                input.firstName(),
                input.lastName(),
                input.email(),
                input.bio(),
                input.birthDate(),
                input.nationality()
        );
        return authorService.create(request);
    }

    /**
     * Обновление автора.
     * Соответствует полю Mutation.updateAuthor(id, input) в схеме.
     */
    @DgsMutation
    public AuthorResponse updateAuthor(@InputArgument String id, @InputArgument UpdateAuthorInputGql input) {
        AuthorRequest request = new AuthorRequest(
                input.firstName(),
                input.lastName(),
                input.email(),
                input.bio(),
                input.birthDate(),
                input.nationality()
        );
        return authorService.update(Long.parseLong(id), request);
    }

    /**
     * Удаление автора и всех его книг (каскадно).
     * Соответствует полю Mutation.deleteAuthor(id) в схеме.
     */
    @DgsMutation
    public boolean deleteAuthor(@InputArgument String id) {
        authorService.delete(Long.parseLong(id));
        return true;
    }
}
