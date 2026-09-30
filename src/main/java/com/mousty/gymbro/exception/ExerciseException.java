package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ExerciseException extends GymBroException {

    private ExerciseException(String message, HttpStatus status) {
        super(message, status);
    }

    public static ExerciseException notFound(UUID id) {
        return new ExerciseException("Exercise not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static ExerciseException unauthorized() {
        return new ExerciseException("Not authorized to perform this action on this exercise", HttpStatus.FORBIDDEN);
    }
}
