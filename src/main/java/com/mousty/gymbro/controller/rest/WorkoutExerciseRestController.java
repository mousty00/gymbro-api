package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.WorkoutExerciseService;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.CurrentUsername;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("workout-exercises")
@RequiredArgsConstructor
@Tag(name = "Workout Exercises", description = "Workout exercise management endpoints")
public class WorkoutExerciseRestController {

    private final WorkoutExerciseService workoutExerciseService;

    @Operation(summary = "Get all workout exercises")
    @GetMapping
    public Connection<WorkoutExerciseDTO> getAllWorkoutExercises(
            @Parameter(description = "Pagination information") @PageableDefault Pageable pageable,
            @Parameter(hidden = true) @CurrentUsername String username) {
        return workoutExerciseService.getAllWorkoutExercises(pageable, username);
    }

    @Operation(summary = "Get workout exercise by ID")
    @GetMapping("/{id}")
    public WorkoutExerciseDTO getWorkoutExerciseById(
            @Parameter(description = "Workout exercise UUID") @PathVariable @NotNull UUID id,
            @Parameter(hidden = true) @CurrentUsername String username) {
        return workoutExerciseService.getWorkoutExerciseById(id, username);
    }

    @Operation(summary = "Update workout exercise")
    @PutMapping("/update")
    public MessageResponse updateWorkoutExercise(
            @Parameter(description = "Workout exercise information") @Valid @RequestBody WorkoutExerciseInput request,
            @Parameter(hidden = true) @CurrentUsername String username) {
        return workoutExerciseService.updateWorkoutExercise(request, username);
    }

    @Operation(summary = "Create workout exercise")
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public EntityResponse<WorkoutExerciseDTO> createWorkoutExercise(
            @Parameter(description = "Workout exercise information") @Valid @RequestBody WorkoutExerciseInput request,
            @Parameter(hidden = true) @CurrentUsername String username) {
        return workoutExerciseService.createWorkoutExercise(request, username);
    }

    @Operation(summary = "Delete workout exercise")
    @DeleteMapping("/delete/{id}")
    public MessageResponse deleteWorkoutExerciseById(
            @Parameter(description = "Workout exercise UUID") @PathVariable @NotNull UUID id,
            @Parameter(hidden = true) @CurrentUsername String username) {
        return workoutExerciseService.deleteWorkoutExerciseById(id, username);
    }
}
