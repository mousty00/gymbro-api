package com.mousty.gymbro.dto.workout_group;

import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.dto.workout.SimpleWorkoutDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record WorkoutGroupDTO(
    @NotNull(message = "id must not be null")
    UUID id,

    @Size(max = 100)
    @NotNull(message = "name is required")
    String name,

    @NotNull
    SimpleWorkoutDTO workout,

    @NotNull
    SimpleUserDTO createdBy,

    Instant scheduledFor,

    @Size(max = 20)
    @NotNull
    String status,

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant createdAt,

    List<GroupMemberDTO> groupMembers
) {}
