package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class LikeException extends GymBroException {

    private LikeException(String message, HttpStatus status) {
        super(message, status);
    }

    public static LikeException notFound(UUID id) {
        return new LikeException("Like not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static LikeException notFound() {
        return new LikeException("Like not found", HttpStatus.NOT_FOUND);
    }

    public static LikeException unauthorized() {
        return new LikeException("Not authorized to perform this action on this like", HttpStatus.FORBIDDEN);
    }

    public static LikeException duplicate() {
        return new LikeException("User has already liked this post", HttpStatus.CONFLICT);
    }
}
