package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseInput;
import com.mousty.gymbro.generic.PageableDefaults;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.CurrentUsername;
import com.mousty.gymbro.service.WorkoutExerciseService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkoutExerciseController {

    private final WorkoutExerciseService service;

    @DgsQuery
    public Connection<WorkoutExerciseDTO> workoutExercises(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size,
            @CurrentUsername String username) {
        return service.getAllWorkoutExercises(PageableDefaults.INSTANCE.create(page, size), username);
    }

    @DgsQuery
    public WorkoutExerciseDTO workoutExercise(@InputArgument UUID id, @CurrentUsername String username) {
        return service.getWorkoutExerciseById(id, username);
    }

    @DgsMutation
    public EntityResponse<WorkoutExerciseDTO> createWorkoutExercise(
            @InputArgument @Valid WorkoutExerciseInput request,
            @CurrentUsername String username) {
        return service.createWorkoutExercise(request, username);
    }

    @DgsMutation
    public MessageResponse updateWorkoutExercise(
            @InputArgument @Valid WorkoutExerciseInput request,
            @CurrentUsername String username) {
        return service.updateWorkoutExercise(request, username);
    }

    @DgsMutation
    public MessageResponse deleteWorkoutExercise(@InputArgument UUID id, @CurrentUsername String username) {
        return service.deleteWorkoutExerciseById(id, username);
    }
}
