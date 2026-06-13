package edu.rutmiit.pochaev.matchmakingapicontract.exception;

import java.util.UUID;

public class LobbyHasPlayersException extends RuntimeException {
    public LobbyHasPlayersException(UUID lobbyId) {
        super("В лобби есть ожидающие игроки, его нельзя распустить вручную: " + lobbyId);
    }
}
