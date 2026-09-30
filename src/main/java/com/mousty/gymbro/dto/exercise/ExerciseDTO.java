package com.mousty.gymbro.dto.exercise;

import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record ExerciseDTO(
    @NotNull
    UUID id,

    @Size(max = 100)
    @NotNull
    String name,

    String description,

    @Size(max = 50)
    @NotNull
    String muscleGroup,

    @NotNull
    Boolean isPublic,

    @NotNull
    UserDTO createdBy,

    List<WorkoutExerciseDTO> workoutExercises
) {}
