package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.generic.PageableDefaults;
import com.mousty.gymbro.service.WorkoutGroupService;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupDTO;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class WorkoutGroupController {

    private final WorkoutGroupService service;
    private final AuthService authService;

    @DgsQuery
    public Connection<WorkoutGroupDTO> workoutGroups(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size
    ) {
        Pageable pageable = PageableDefaults.INSTANCE.create(page, size);
        return service.getAllWorkoutGroups(pageable);
    }

    @DgsQuery
    public WorkoutGroupDTO workoutGroup(
        @InputArgument @NotNull UUID id
    ){
        return service.getWorkoutGroupById(id);
    }

    @DgsMutation
    public EntityResponse<WorkoutGroupDTO> createWorkoutGroup(
            @InputArgument @NotNull @Valid WorkoutGroupInput request
            ){
        return service.createWorkoutGroup(request,authService.getCurrentUsername() );
    }

    @DgsMutation
    public MessageResponse deleteWorkoutGroup(
            @InputArgument @NotNull UUID id
    ){
        return service.deleteWorkoutGroup(id,authService.getCurrentUsername());
    }


}
