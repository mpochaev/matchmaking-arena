package edu.rutmiit.demo.demorest.controllers;

import edu.rutmiit.demo.booksapicontract.dto.*;
import edu.rutmiit.demo.booksapicontract.endpoints.AuthorApi;
import edu.rutmiit.demo.demorest.assemblers.AuthorModelAssembler;
import edu.rutmiit.demo.demorest.assemblers.BookModelAssembler;
import edu.rutmiit.demo.demorest.service.AuthorService;
import edu.rutmiit.demo.demorest.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorController implements AuthorApi {

    private final AuthorService authorService;
    private final BookService bookService;
    private final AuthorModelAssembler authorModelAssembler;
    private final BookModelAssembler bookModelAssembler;
    private final PagedResourcesAssembler<AuthorResponse> pagedAuthorsAssembler;
    private final PagedResourcesAssembler<BookResponse> pagedBooksAssembler;

    public AuthorController(AuthorService authorService,
                            BookService bookService,
                            AuthorModelAssembler authorModelAssembler,
                            BookModelAssembler bookModelAssembler,
                            PagedResourcesAssembler<AuthorResponse> pagedAuthorsAssembler,
                            PagedResourcesAssembler<BookResponse> pagedBooksAssembler) {
        this.authorService = authorService;
        this.bookService = bookService;
        this.authorModelAssembler = authorModelAssembler;
        this.bookModelAssembler = bookModelAssembler;
        this.pagedAuthorsAssembler = pagedAuthorsAssembler;
        this.pagedBooksAssembler = pagedBooksAssembler;
    }

    @Override
    public PagedModel<EntityModel<AuthorResponse>> getAllAuthors(int page, int size) {
        PagedResponse<AuthorResponse> paged = authorService.findAll(page, size);
        Page<AuthorResponse> springPage = new PageImpl<>(
                paged.content(),
                PageRequest.of(paged.pageNumber(), paged.pageSize()),
                paged.totalElements()
        );
        return pagedAuthorsAssembler.toModel(springPage, authorModelAssembler);
    }

    @Override
    public EntityModel<AuthorResponse> getAuthorById(Long id) {
        return authorModelAssembler.toModel(authorService.findById(id));
    }

    @Override
    public ResponseEntity<EntityModel<AuthorResponse>> createAuthor(AuthorRequest request) {
        AuthorResponse created = authorService.create(request);
        EntityModel<AuthorResponse> model = authorModelAssembler.toModel(created);
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }

    @Override
    public EntityModel<AuthorResponse> updateAuthor(Long id, AuthorRequest request) {
        return authorModelAssembler.toModel(authorService.update(id, request));
    }

    @Override
    public EntityModel<AuthorResponse> patchAuthor(Long id, PatchAuthorRequest request) {
        return authorModelAssembler.toModel(authorService.patchAuthor(id, request));
    }

    @Override
    public void deleteAuthor(Long id) {
        authorService.delete(id);
    }

    @Override
    public PagedModel<EntityModel<BookResponse>> getBooksByAuthor(Long id, int page, int size) {
        // Проверяем что автор существует (выбросит 404 если нет)
        authorService.findById(id);
        PagedResponse<BookResponse> paged = bookService.findAllBooks(id, null, null, null, page, size);
        Page<BookResponse> springPage = new PageImpl<>(
                paged.content(),
                PageRequest.of(paged.pageNumber(), paged.pageSize()),
                paged.totalElements()
        );
        return pagedBooksAssembler.toModel(springPage, bookModelAssembler);
    }
}
