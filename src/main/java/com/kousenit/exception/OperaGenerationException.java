package com.kousenit.exception;

/**
 * Base exception for all opera generation related errors.
 */
public class OperaGenerationException extends Exception {

    public OperaGenerationException(String message) {
        super(message);
    }

    public OperaGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperaGenerationException(Throwable cause) {
        super(cause);
    }
}