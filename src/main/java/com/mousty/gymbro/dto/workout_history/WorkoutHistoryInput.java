package com.mousty.gymbro.dto.workout_history;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutHistoryInput {
    @NotNull
    private UUID workoutId;

    private UUID groupId;

    private String notes;

    @NotNull
    private Instant startedAt;

    private Instant completedAt;
}

