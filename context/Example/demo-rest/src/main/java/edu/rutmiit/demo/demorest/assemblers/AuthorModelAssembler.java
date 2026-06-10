package edu.rutmiit.demo.demorest.assemblers;

import edu.rutmiit.demo.booksapicontract.dto.AuthorResponse;
import edu.rutmiit.demo.demorest.controllers.AuthorController;
import edu.rutmiit.demo.demorest.controllers.BookController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class AuthorModelAssembler implements RepresentationModelAssembler<AuthorResponse, EntityModel<AuthorResponse>> {

    @Override
    public EntityModel<AuthorResponse> toModel(AuthorResponse author) {
        return EntityModel.of(author,
                linkTo(methodOn(AuthorController.class).getAuthorById(author.getId())).withSelfRel(),
                linkTo(methodOn(BookController.class).getAllBooks(author.getId(), null, null, null, 0, 20)).withRel("books"),
                linkTo(methodOn(AuthorController.class).getAllAuthors(0, 20)).withRel("collection")
        );
    }
}