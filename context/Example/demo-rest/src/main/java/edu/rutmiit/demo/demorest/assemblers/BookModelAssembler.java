package edu.rutmiit.demo.demorest.assemblers;

import edu.rutmiit.demo.booksapicontract.dto.BookResponse;
import edu.rutmiit.demo.demorest.controllers.AuthorController;
import edu.rutmiit.demo.demorest.controllers.BookController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class BookModelAssembler implements RepresentationModelAssembler<BookResponse, EntityModel<BookResponse>> {

    @Override
    public EntityModel<BookResponse> toModel(BookResponse book) {
        EntityModel<BookResponse> model = EntityModel.of(book,
                linkTo(methodOn(BookController.class).getBookById(book.getId())).withSelfRel(),
                linkTo(methodOn(BookController.class).getAllBooks(null, null, null, null, 0, 20)).withRel("collection")
        );
        if (book.getAuthor() != null) {
            model.add(linkTo(methodOn(AuthorController.class)
                    .getAuthorById(book.getAuthor().getId())).withRel("author"));
        }
        return model;
    }
}
