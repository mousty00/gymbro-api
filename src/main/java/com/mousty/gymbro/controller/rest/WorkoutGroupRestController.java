package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.WorkoutGroupService;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupDTO;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/workout_groups")
@RequiredArgsConstructor
@Tag(name = "Workout Groups", description = "APIs for managing workout groups")
public class WorkoutGroupRestController {

    private final WorkoutGroupService service;

    @Operation(summary = "Get all workout groups", description = "Returns a paginated list of all workout groups")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved workout groups")
    @GetMapping
    public Connection<WorkoutGroupDTO> getAllWorkoutGroups(@PageableDefault Pageable pageable) {
        return service.getAllWorkoutGroups(pageable);
    }

    @Operation(summary = "Get workout group by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workout group found"),
            @ApiResponse(responseCode = "404", description = "Workout group not found")
    })
    @GetMapping("/{id}")
    public WorkoutGroupDTO getWorkoutGroupById(@PathVariable @Parameter(description = "Workout group UUID") UUID id) {
        return service.getWorkoutGroupById(id);
    }

    @Operation(summary = "Create new workout group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Workout group created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public EntityResponse<WorkoutGroupDTO> createWorkoutGroup(
            @RequestBody @Parameter(description = "Workout group details") WorkoutGroupInput request,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name") String username) {
        return service.createWorkoutGroup(request, username);
    }

    @Operation(summary = "Delete workout group" )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Workout group deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @DeleteMapping("/delete/{id}")
    public MessageResponse deleteWorkoutGroup(
            @Valid @NotNull @Parameter(description = "Workout group id")
            @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentSecurityContext(expression = "authentication?.name") String username
    ){
        return service.deleteWorkoutGroup(id, username);
    }

}