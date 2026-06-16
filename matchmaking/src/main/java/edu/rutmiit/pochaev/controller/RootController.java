package edu.rutmiit.pochaev.controller;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api")
public class RootController {

    @GetMapping
    public RepresentationModel<?> getRoot() {
        RepresentationModel<?> rootModel = new RepresentationModel<>();
        rootModel.add(
                linkTo(methodOn(PlayerController.class).getAllPlayers(null, null, 0, 20)).withRel("players"),
                linkTo(methodOn(LobbyController.class).getAllLobbies(null, null, null, null, 0, 20)).withRel("lobbies"),
                linkTo(methodOn(MatchController.class).getAllMatches(null, null, null, null, 0, 20)).withRel("matches")
        );
        return rootModel;
    }
}
