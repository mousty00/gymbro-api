package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.generic.PageableDefaults;
import com.mousty.gymbro.service.WorkoutHistoryService;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryDTO;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryInput;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class WorkoutHistoryController {

    private final WorkoutHistoryService service;
    private final AuthService authService;

    @DgsQuery
    public Connection<WorkoutHistoryDTO> workoutHistories(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size
    ) {
        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 15
        );
        return service.getUserWorkoutHistories(authService.getCurrentUsername(), pageable);
    }

    @DgsQuery
    public Connection<WorkoutHistoryDTO> GroupWorkoutHistories(
            @InputArgument UUID groupId,
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size
    ) {
        Pageable pageable = PageableDefaults.INSTANCE.create(page, size);
        return service.getGroupWorkoutHistories(groupId, pageable);
    }

    @DgsQuery
    public WorkoutHistoryDTO workoutHistory(
            @InputArgument @NotNull @Valid UUID id
    ) {
        return service.getWorkoutHistoryById(id);
    }

    @DgsMutation
    public EntityResponse<WorkoutHistoryDTO> createWorkoutHistory(
            @InputArgument @NotNull @Valid WorkoutHistoryInput request
    ) {
        return service.createWorkoutHistory(request, authService.getCurrentUsername());
    }

    @DgsMutation
    public MessageResponse updateWorkoutHistory(
            @InputArgument @NotNull @Valid UUID id,
            @InputArgument @NotNull @Valid WorkoutHistoryInput request
    ){
        return service.updateWorkoutHistory(id, request, authService.getCurrentUsername());
    }

    @DgsMutation
    public MessageResponse deleteWorkoutHistory(
            @InputArgument @NotNull @Valid UUID id
    ){
        return service.deleteWorkoutHistory(id, authService.getCurrentUsername());
    }


}
