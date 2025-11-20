package com.mousty.gymbro.dto.workout_history;

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
public class WorkoutHistoryDTO {
    private UUID id;
    private UUID userId;
    private UUID workoutId;
    private UUID groupId;
    private Instant startedAt;
    private Instant completedAt;
    private String notes;
}
