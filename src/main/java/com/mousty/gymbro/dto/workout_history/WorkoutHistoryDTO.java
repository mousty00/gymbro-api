package com.mousty.gymbro.dto.workout_history;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record WorkoutHistoryDTO(
    UUID id,
    UUID userId,
    UUID workoutId,
    UUID groupId,
    Instant startedAt,
    Instant completedAt,
    String notes
) {}
