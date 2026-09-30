package com.mousty.gymbro.dto.workout_exercise;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder(toBuilder = true)
public record WorkoutExerciseDTO(
    @NotNull
    UUID id,

    @NotNull
    UUID workoutId,

    @NotNull
    UUID exerciseId,

    String exerciseName,

    @NotNull
    Integer sets,

    @NotNull
    Integer reps,

    BigDecimal weight,

    @NotNull
    Integer restSeconds,

    @NotNull
    Integer position
) {}
