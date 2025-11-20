package com.mousty.gymbro.dto.workout_exercise;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutExerciseDTO {
    @NotNull
    private UUID id;

    @NotNull
    private UUID workoutId;

    @NotNull
    private UUID exerciseId;

    private String exerciseName;

    @NotNull
    private Integer sets;

    @NotNull
    private Integer reps;

    private BigDecimal weight;

    @NotNull
    private Integer restSeconds;

    @NotNull
    private Integer position;
}
