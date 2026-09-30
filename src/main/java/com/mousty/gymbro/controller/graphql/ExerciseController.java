package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.ExerciseInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.service.ExerciseService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.mousty.gymbro.generic.PageableDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mousty.gymbro.security.CurrentUsername;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService service;

    @DgsQuery
    public Connection<ExerciseDTO> exercises(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size,
            @CurrentUsername String username) {
        return service.getAllExercises(PageableDefaults.INSTANCE.create(page, size), username);
    }

    @DgsQuery
    public ExerciseDTO exercise(@InputArgument UUID id, @CurrentUsername String username) {
        return service.getExerciseById(id, username);
    }

    @PreAuthorize("isAuthenticated()")
    @DgsMutation
    public MessageResponse deleteExercise(
            @InputArgument UUID id,
            @CurrentUsername String username) {
        return service.deleteExerciseById(id, username);
    }

    @PreAuthorize("isAuthenticated()")
    @DgsMutation
    public MessageResponse updateExercise(
            @InputArgument @Valid ExerciseInput request,
            @CurrentUsername String username) {
        return service.updateExercise(request, username);
    }

    @PreAuthorize("isAuthenticated()")
    @DgsMutation
    public EntityResponse<ExerciseDTO> createExercise(
            @InputArgument @Valid ExerciseInput request,
            @CurrentUsername String username) {
        return service.createExercise(request, username);
    }
}
