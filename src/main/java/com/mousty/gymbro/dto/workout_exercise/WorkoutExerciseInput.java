package com.mousty.gymbro.dto.workout_exercise;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder(toBuilder = true)
public record WorkoutExerciseInput(
    UUID id,

    @NotNull
    UUID workoutId,

    @NotNull
    UUID exerciseId,

    @NotNull
    Integer sets,

    Integer reps,

    BigDecimal weight,

    Integer restSeconds,

    @NotNull
    Integer position
) {}
