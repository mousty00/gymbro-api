package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.service.WorkoutService;
import com.mousty.gymbro.dto.workout.WorkoutDTO;
import com.mousty.gymbro.dto.workout.WorkoutInput;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService service;
    private final AuthService authService;

    @DgsQuery
    public List<WorkoutDTO> workouts() {
        return service.getWorkouts(authService.getCurrentUsername());
    }

    @DgsQuery
    public WorkoutDTO workout(
            @InputArgument @NotNull @Valid UUID id
    ) {
        return service.getWorkoutById(id);
    }

    @DgsMutation
    public MessageResponse deleteWorkout(
            @InputArgument @NotNull @Valid UUID id
    ) {
        return service.deleteWorkoutById(id);
    }

    @DgsMutation
    public MessageResponse updateWorkout(
            @InputArgument @NotNull @Valid WorkoutInput request
    ) {
        return service.updateWorkout(request);
    }

    @DgsMutation
    public EntityResponse<WorkoutDTO> createWorkout(
            @InputArgument @NotNull @Valid WorkoutInput request
    ){
        return service.createWorkout(request);
    }

}
