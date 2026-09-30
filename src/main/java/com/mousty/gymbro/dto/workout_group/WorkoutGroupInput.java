package com.mousty.gymbro.dto.workout_group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record WorkoutGroupInput(
    @Size(max = 100)
    @NotNull(message = "name is required")
    String name,

    @NotNull
    UUID workoutId,

    Instant scheduledFor,

    @Size(max = 20)
    @NotNull
    String status
) {}
