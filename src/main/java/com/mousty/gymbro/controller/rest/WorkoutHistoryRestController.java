package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.WorkoutHistoryService;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryDTO;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/workout-history")
@RequiredArgsConstructor
@Tag(name = "Workout History", description = "APIs for managing workout history")
public class WorkoutHistoryRestController {

    private final WorkoutHistoryService service;

    @Operation(summary = "Get all workout histories", description = "Returns a paginated list of all workout histories")
    @GetMapping
    public Connection<WorkoutHistoryDTO> getAllWorkoutHistories(@PageableDefault Pageable pageable) {
        return service.getAllWorkoutHistories(pageable);
    }

    @Operation(summary = "Get user's workout histories", description = "Returns a paginated list of workout histories for a specific user")
    @GetMapping("/user")
    public Connection<WorkoutHistoryDTO> getUserWorkoutHistories(
            @PageableDefault Pageable pageable,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name") String username) {
        return service.getUserWorkoutHistories(username, pageable);
    }

    @Operation(summary = "Get group's workout histories", description = "Returns a paginated list of workout histories for a specific group")
    @GetMapping("/group/{groupId}")
    public Connection<WorkoutHistoryDTO> getGroupWorkoutHistories(
            @Parameter(description = "Group ID") @PathVariable UUID groupId,
            @PageableDefault Pageable pageable) {
        return service.getGroupWorkoutHistories(groupId, pageable);
    }

    @Operation(summary = "Get workout history by ID")
    @GetMapping("/{id}")
    public WorkoutHistoryDTO getWorkoutHistoryById(
            @Parameter(description = "Workout history ID") @PathVariable UUID id) {
        return service.getWorkoutHistoryById(id);
    }

    @Operation(summary = "Create workout history")
    @PostMapping
    public EntityResponse<WorkoutHistoryDTO> createWorkoutHistory(
            @Parameter(description = "Workout history details") @Valid @RequestBody WorkoutHistoryInput input,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name") String username) {
        return service.createWorkoutHistory(input, username);
    }

    @Operation(summary = "Update workout history")
    @PutMapping("update/{id}")
    public MessageResponse updateWorkoutHistory(
            @Parameter(description = "Workout history ID") @PathVariable UUID id,
            @Parameter(description = "Updated workout history details") @Valid @RequestBody WorkoutHistoryInput input,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name") String username) {
        return service.updateWorkoutHistory(id, input, username);
    }

    @Operation(summary = "Delete workout history")
    @DeleteMapping("delete/{id}")
    public MessageResponse deleteWorkoutHistory(
            @Parameter(description = "Workout history ID") @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name") String username) {
        return service.deleteWorkoutHistory(id, username);
    }
}