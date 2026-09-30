package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class WorkoutExerciseException extends GymBroException {

    private WorkoutExerciseException(String message, HttpStatus status) {
        super(message, status);
    }

    public static WorkoutExerciseException notFound(UUID id) {
        return new WorkoutExerciseException("Workout exercise not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static WorkoutExerciseException notFound() {
        return new WorkoutExerciseException("Workout exercise not found", HttpStatus.NOT_FOUND);
    }

    public static WorkoutExerciseException idRequired() {
        return new WorkoutExerciseException("Workout exercise ID is required", HttpStatus.BAD_REQUEST);
    }
}
