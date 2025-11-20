package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.WorkoutService;
import com.mousty.gymbro.dto.workout.WorkoutDTO;
import com.mousty.gymbro.dto.workout.WorkoutInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workouts")
@RequiredArgsConstructor
@Tag(name = "Workouts", description = "Workout management endpoints")
public class WorkoutRestController {

    private final WorkoutService workoutService;

    @Operation(summary = "Get all workouts", description = "Returns a paginated list of all workouts")
    @GetMapping
    public Connection<WorkoutDTO> getAllWorkouts(
            @Parameter(description = "Pagination parameters")
            @PageableDefault Pageable pageable) {
        return workoutService.getAllWorkouts(pageable);
    }

    @Operation(summary = "Get all workout by logged user", description = "Returns a paginated list of all workouts")
    @GetMapping("/profile")
    public List<WorkoutDTO> getWorkouts(
            @Parameter(hidden = true)
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return workoutService.getWorkouts(username);
    }

    @Operation(summary = "get workout by id", description = "get a workout by its UUID")
    @GetMapping("/{id}")
    public WorkoutDTO getWorkoutById(
            @Parameter(description = "Workout UUID")
            @PathVariable UUID id) {
        return workoutService.getWorkoutById(id);
    }

    @Operation(summary = "Delete workout", description = "Deletes a workout by its UUID")
    @DeleteMapping("delete/{id}")
    public MessageResponse deleteWorkoutById(
            @Parameter(description = "Workout UUID")
            @PathVariable UUID id) {
        return workoutService.deleteWorkoutById(id);
    }

    @Operation(summary = "Update workout", description = "Updates an existing workout's information")
    @PutMapping("/update")
    public MessageResponse updateWorkoutById(
            @Parameter(description = "Updated workout information")
            @Valid @RequestBody WorkoutInput request) {
        return workoutService.updateWorkout(request);
    }

    @Operation(summary = "Create workout", description = "Creates a new workout")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201")
    @PostMapping("/create")
    public EntityResponse<WorkoutDTO> createWorkout(
            @Parameter(description = "New workout information")
            @Valid @RequestBody WorkoutInput request) {
        return workoutService.createWorkout(request);
    }
}
