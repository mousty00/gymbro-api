package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.service.ExerciseService;
import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.ExerciseInput;
import com.mousty.gymbro.pagination.Connection;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.CurrentSecurityContext;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService service;

    @DgsQuery
    public Connection<ExerciseDTO> exercises(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size) {
        return service.getAllExercises(PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 15));
    }

    @DgsQuery
    public ExerciseDTO exercise(@InputArgument UUID id) {
        return service.getExerciseById(id);
    }

    @DgsMutation
    public MessageResponse deleteExercise(
            @InputArgument UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.deleteExerciseById(id, username);
    }

    @DgsMutation
    public MessageResponse updateExercise(
            @InputArgument @Valid ExerciseInput request,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.updateExercise(request, username);
    }



}
