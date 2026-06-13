package edu.rutmiit.pochaev.matchmakingapicontract.exception;

public class PlayerAlreadyInLobbyException extends RuntimeException {
    public PlayerAlreadyInLobbyException(String message) {
        super(message);
    }
}
