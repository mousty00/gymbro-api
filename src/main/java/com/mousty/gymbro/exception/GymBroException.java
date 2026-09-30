package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

public abstract class GymBroException extends RuntimeException {
    private final HttpStatus status;

    protected GymBroException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
