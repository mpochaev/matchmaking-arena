package edu.rutmiit.pochaev.assembler;

import edu.rutmiit.pochaev.controller.LobbyController;
import edu.rutmiit.pochaev.controller.PlayerController;
import edu.rutmiit.pochaev.matchmakingapicontract.dto.PlayerResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PlayerModelAssembler implements RepresentationModelAssembler<PlayerResponse, EntityModel<PlayerResponse>> {

    @Override
    public EntityModel<PlayerResponse> toModel(PlayerResponse player) {
        return EntityModel.of(player,
                linkTo(methodOn(PlayerController.class).getPlayerById(player.id())).withSelfRel(),
                linkTo(methodOn(PlayerController.class).getAllPlayers(null, null, 0, 20)).withRel("collection"),
                linkTo(methodOn(LobbyController.class).joinLobby(null)).withRel("join-lobby"),
                linkTo(methodOn(PlayerController.class).deletePlayer(player.id())).withRel("delete")
        );
    }
}
