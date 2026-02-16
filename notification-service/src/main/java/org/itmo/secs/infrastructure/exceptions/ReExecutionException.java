package org.itmo.secs.infrastructure.exceptions;

public class ReExecutionException extends RuntimeException {
    public ReExecutionException(String message) {
        super(message);
    }
}
