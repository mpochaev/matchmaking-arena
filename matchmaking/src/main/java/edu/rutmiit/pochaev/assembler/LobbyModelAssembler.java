package edu.rutmiit.pochaev.assembler;

import edu.rutmiit.pochaev.controller.LobbyController;
import edu.rutmiit.pochaev.controller.MatchController;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.LobbyResponse;
import edu.rutmiit.pochaev.matchmakingapicontract.enums.LobbyStatus;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class LobbyModelAssembler implements RepresentationModelAssembler<LobbyResponse, EntityModel<LobbyResponse>> {

    @Override
    public EntityModel<LobbyResponse> toModel(LobbyResponse lobby) {
        EntityModel<LobbyResponse> model = EntityModel.of(lobby,
                linkTo(methodOn(LobbyController.class).getLobbyById(lobby.id())).withSelfRel(),
                linkTo(methodOn(LobbyController.class).getAllLobbies(null, null, null, null, 0, 20)).withRel("collection"),
                linkTo(methodOn(LobbyController.class).joinLobby(null)).withRel("join-lobby")
        );

        if (lobby.status() == LobbyStatus.WAITING) {
            model.add(linkTo(methodOn(LobbyController.class).disbandLobby(lobby.id())).withRel("disband"));
        }
        if (lobby.matchId() != null) {
            model.add(linkTo(methodOn(MatchController.class).getMatchById(lobby.matchId())).withRel("match"));
        }
        return model;
    }
}
