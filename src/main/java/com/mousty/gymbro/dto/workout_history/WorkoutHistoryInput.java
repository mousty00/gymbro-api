package com.mousty.gymbro.dto.workout_history;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record WorkoutHistoryInput(
    @NotNull
    UUID workoutId,

    UUID groupId,

    String notes,

    @NotNull
    Instant startedAt,

    Instant completedAt
) {}
