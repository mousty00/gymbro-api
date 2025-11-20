package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.ExerciseService;
import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.ExerciseInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.validation.Put;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises", description = "APIs for managing exercises")
public class ExerciseRestController {

    private final ExerciseService exerciseService;

    @Operation(summary = "Get all exercises", description = "Returns a paginated list of exercises")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved exercises")
    @GetMapping
    public Connection<ExerciseDTO> getAllExercises(@PageableDefault Pageable pageable) {
        return exerciseService.getAllExercises(pageable);
    }


    @Operation(summary = "Get exercise by ID")
    @ApiResponse(responseCode = "200", description = "Exercise found")
    @ApiResponse(responseCode = "404", description = "Exercise not found")
    @GetMapping("{id}")
    public ExerciseDTO getExerciseById(@Parameter(description = "Exercise ID") @PathVariable UUID id) {
        return exerciseService.getExerciseById(id);
    }


    @Operation(summary = "Delete exercise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized to delete exercise"),
            @ApiResponse(responseCode = "404", description = "Exercise not found")
    })
    @DeleteMapping("delete/{id}")
    public MessageResponse deleteExerciseById(
            @Parameter(description = "Exercise ID") @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name") String username) {
        return exerciseService.deleteExerciseById(id, username);
    }


    @Operation(summary = "update exercise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise updated successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized to update exercise"),
            @ApiResponse(responseCode = "404", description = "Exercise not found")
    })
    @PutMapping("/update")
    public MessageResponse updateExercise(
            @Parameter(description = "Exercise details") @Valid @RequestBody ExerciseInput request,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name") String username) {
        return exerciseService.updateExercise(request, username);
    }

    @Operation(summary = "create exercise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise updated successfully"),
            @ApiResponse(responseCode = "403", description = "Unauthorized to update exercise"),
            @ApiResponse(responseCode = "404", description = "Exercise not found")
    })
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<EntityResponse<ExerciseDTO>> createExercise(
            @Parameter(description = "Exercise details") @Validated(value = Put.class) @RequestBody ExerciseInput request,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return exerciseService.createExercise(request, username);
    }


}
