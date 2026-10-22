package com.example.starter.domain.exception.technical;

public abstract class TechnicalException extends RuntimeException {
    protected TechnicalException(String message) {
        super(message);
    }

    protected TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
