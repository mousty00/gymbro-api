package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class WorkoutException extends GymBroException {

    private WorkoutException(String message, HttpStatus status) {
        super(message, status);
    }

    public static WorkoutException notFound(UUID id) {
        return new WorkoutException("Workout not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static WorkoutException notFound(String identifier) {
        return new WorkoutException("Workout not found: " + identifier, HttpStatus.NOT_FOUND);
    }

    public static WorkoutException unauthorized() {
        return new WorkoutException("Not authorized to perform this action on this workout", HttpStatus.FORBIDDEN);
    }

    public static WorkoutException idRequired() {
        return new WorkoutException("Workout ID is required", HttpStatus.BAD_REQUEST);
    }
}
