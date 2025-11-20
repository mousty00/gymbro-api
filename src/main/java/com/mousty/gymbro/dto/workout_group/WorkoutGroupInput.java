package com.mousty.gymbro.dto.workout_group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutGroupInput {

    @Size(max = 100)
    @NotNull(message = "name is required")
    private String name;

    @NotNull
    private UUID workoutId;

    @NotNull
    private UUID createdById;

    private Instant scheduledFor;

    @Size(max = 20)
    @NotNull
    private String status;

    private List<UUID> groupMembersIds;

}
