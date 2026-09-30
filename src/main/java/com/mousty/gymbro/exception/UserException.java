package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserException extends GymBroException {

    private UserException(String message, HttpStatus status) {
        super(message, status);
    }

    public static UserException notFound(String identifier) {
        return new UserException("User not found: " + identifier, HttpStatus.NOT_FOUND);
    }

    public static UserException notFound(UUID id) {
        return new UserException("User not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static UserException alreadyExists(String field, String value) {
        return new UserException(field + " '" + value + "' already exists", HttpStatus.CONFLICT);
    }

    public static UserException unauthorized() {
        return new UserException("Not authorized to perform this action on this user", HttpStatus.FORBIDDEN);
    }
}
