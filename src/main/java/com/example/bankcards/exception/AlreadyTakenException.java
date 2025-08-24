package com.example.bankcards.exception;

public class AlreadyTakenException extends RuntimeException {
    public AlreadyTakenException() {
    }

    public AlreadyTakenException(String message) {
        super(message);
    }

    public AlreadyTakenException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyTakenException(Throwable cause) {
        super(cause);
    }

    public AlreadyTakenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
