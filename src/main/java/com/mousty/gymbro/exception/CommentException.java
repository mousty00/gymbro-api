package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CommentException extends GymBroException {

    private CommentException(String message, HttpStatus status) {
        super(message, status);
    }

    public static CommentException notFound(UUID id) {
        return new CommentException("Comment not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static CommentException notFound() {
        return new CommentException("Comment not found", HttpStatus.NOT_FOUND);
    }

    public static CommentException unauthorized() {
        return new CommentException("Not authorized to perform this action on this comment", HttpStatus.FORBIDDEN);
    }
}
