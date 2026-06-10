package edu.rutmiit.demo.demorest.controllers;

import edu.rutmiit.demo.booksapicontract.dto.*;
import edu.rutmiit.demo.booksapicontract.endpoints.BookApi;
import edu.rutmiit.demo.demorest.assemblers.BookModelAssembler;
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
public class BookController implements BookApi {

    private final BookService bookService;
    private final BookModelAssembler bookModelAssembler;
    private final PagedResourcesAssembler<BookResponse> pagedResourcesAssembler;

    public BookController(BookService bookService, BookModelAssembler bookModelAssembler,
                          PagedResourcesAssembler<BookResponse> pagedResourcesAssembler) {
        this.bookService = bookService;
        this.bookModelAssembler = bookModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public EntityModel<BookResponse> getBookById(Long id) {
        return bookModelAssembler.toModel(bookService.findBookById(id));
    }

    @Override
    public PagedModel<EntityModel<BookResponse>> getAllBooks(Long authorId, String genre, Integer publishedYear,
                                                            String titleSearch, int page, int size) {
        PagedResponse<BookResponse> paged = bookService.findAllBooks(authorId, genre, publishedYear, titleSearch, page, size);
        Page<BookResponse> springPage = new PageImpl<>(
                paged.content(),
                PageRequest.of(paged.pageNumber(), paged.pageSize()),
                paged.totalElements()
        );
        return pagedResourcesAssembler.toModel(springPage, bookModelAssembler);
    }

    @Override
    public ResponseEntity<EntityModel<BookResponse>> createBook(BookRequest request) {
        BookResponse created = bookService.createBook(request);
        EntityModel<BookResponse> model = bookModelAssembler.toModel(created);
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }

    @Override
    public EntityModel<BookResponse> updateBook(Long id, UpdateBookRequest request) {
        return bookModelAssembler.toModel(bookService.updateBook(id, request));
    }

    @Override
    public EntityModel<BookResponse> patchBook(Long id, PatchBookRequest request) {
        return bookModelAssembler.toModel(bookService.patchBook(id, request));
    }

    @Override
    public void deleteBook(Long id) {
        bookService.deleteBook(id);
    }
}