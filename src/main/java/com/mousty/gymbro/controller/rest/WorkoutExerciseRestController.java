package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.WorkoutExerciseService;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("workout-exercises")
@RequiredArgsConstructor
@Tag(name = "Workout Exercises", description = "Workout exercise management endpoints")
public class WorkoutExerciseRestController {

    private final WorkoutExerciseService workoutExerciseService;

    @Operation(summary = "Get all workout exercises", description = "Returns a paginated list of all workout exercises")
    @GetMapping
    public Connection<WorkoutExerciseDTO> getAllWorkoutExercises(
            @Parameter(description = "Pagination information") @PageableDefault Pageable pageable) {
        return workoutExerciseService.getAllWorkoutExercises(pageable);
    }

    @Operation(summary = "Get workout exercise by ID", description = "Returns a single workout exercise by its UUID")
    @GetMapping("/{id}")
    public WorkoutExerciseDTO getWorkoutExerciseById(
            @Parameter(description = "Workout exercise UUID") @PathVariable @NotNull UUID id) {
        return workoutExerciseService.getWorkoutExerciseById(id);
    }

    @Operation(summary = "Update workout exercise", description = "Updates an existing workout exercise")
    @PutMapping("/update")
    public ResponseEntity<MessageResponse> updateWorkoutExercise(
            @Parameter(description = "Workout exercise information") @Valid @RequestBody WorkoutExerciseInput request) {
        return workoutExerciseService.updateWorkoutExercise(request);
    }

    @Operation(summary = "Create workout exercise", description = "Creates a new workout exercise")
    @PostMapping("/create")
    public ResponseEntity<EntityResponse<WorkoutExerciseDTO>> createWorkoutExercise(
            @Parameter(description = "Workout exercise information") @Valid @RequestBody WorkoutExerciseInput request) {
        return workoutExerciseService.createWorkoutExercise(request);
    }

    @Operation(summary = "Delete workout exercise", description = "Deletes a workout exercise by its UUID")
    @DeleteMapping("/delete/{id}")
    public MessageResponse deleteWorkoutExerciseById(
            @Parameter(description = "Workout exercise UUID") @PathVariable @NotNull UUID id) {
        return workoutExerciseService.deleteWorkoutExerciseById(id);
    }


}
