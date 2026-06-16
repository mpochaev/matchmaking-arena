package edu.rutmiit.pochaev.assembler;

import edu.rutmiit.pochaev.controller.LobbyController;
import edu.rutmiit.pochaev.controller.MatchController;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.MatchResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MatchModelAssembler implements RepresentationModelAssembler<MatchResponse, EntityModel<MatchResponse>> {

    @Override
    public EntityModel<MatchResponse> toModel(MatchResponse match) {
        return EntityModel.of(match,
                linkTo(methodOn(MatchController.class).getMatchById(match.id())).withSelfRel(),
                linkTo(methodOn(MatchController.class).getAllMatches(null, null, null, null, 0, 20)).withRel("collection"),
                linkTo(methodOn(LobbyController.class).getLobbyById(match.lobbyId())).withRel("source-lobby")
        );
    }
}
