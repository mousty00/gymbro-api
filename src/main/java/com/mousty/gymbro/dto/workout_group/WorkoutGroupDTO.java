package com.mousty.gymbro.dto.workout_group;

import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.dto.workout.SimpleWorkoutDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutGroupDTO {

    @NotNull(message = "id must not be null")
    private UUID id;

    @Size(max = 100)
    @NotNull(message = "name is required")
    private String name;

    @NotNull
    private SimpleWorkoutDTO workout;

    @NotNull
    private SimpleUserDTO createdBy;

    private Instant scheduledFor;

    @Size(max = 20)
    @NotNull
    private String status;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAt;

    private List<GroupMemberDTO> groupMembers;

}
