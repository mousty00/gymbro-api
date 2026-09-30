package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class WorkoutGroupException extends GymBroException {

    private WorkoutGroupException(String message, HttpStatus status) {
        super(message, status);
    }

    public static WorkoutGroupException notFound(UUID id) {
        return new WorkoutGroupException("Workout group not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static WorkoutGroupException notFound(String identifier) {
        return new WorkoutGroupException("Workout group not found: " + identifier, HttpStatus.NOT_FOUND);
    }

    public static WorkoutGroupException unauthorized() {
        return new WorkoutGroupException("Not authorized to perform this action on this workout group", HttpStatus.FORBIDDEN);
    }
}
