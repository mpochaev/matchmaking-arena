package edu.rutmiit.pochaev.matchmakingapicontract.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
