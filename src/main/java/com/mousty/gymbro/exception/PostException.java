package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PostException extends GymBroException {

    private PostException(String message, HttpStatus status) {
        super(message, status);
    }

    public static PostException notFound(UUID id) {
        return new PostException("Post not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static PostException notFound() {
        return new PostException("Post not found", HttpStatus.NOT_FOUND);
    }

    public static PostException unauthorized() {
        return new PostException("Not authorized to perform this action on this post", HttpStatus.FORBIDDEN);
    }

    public static PostException userIdMismatch() {
        return new PostException("User ID mismatch", HttpStatus.BAD_REQUEST);
    }
}
