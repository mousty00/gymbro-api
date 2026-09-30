package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class WorkoutHistoryException extends GymBroException {

    private WorkoutHistoryException(String message, HttpStatus status) {
        super(message, status);
    }

    public static WorkoutHistoryException notFound(UUID id) {
        return new WorkoutHistoryException("Workout history not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static WorkoutHistoryException notFound() {
        return new WorkoutHistoryException("Workout history not found", HttpStatus.NOT_FOUND);
    }

    public static WorkoutHistoryException unauthorized() {
        return new WorkoutHistoryException("Not authorized to perform this action on this workout history", HttpStatus.FORBIDDEN);
    }
}
