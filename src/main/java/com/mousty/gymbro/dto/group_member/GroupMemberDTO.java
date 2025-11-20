package com.mousty.gymbro.dto.group_member;

import com.mousty.gymbro.dto.user.SimpleUserDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberDTO {
    @NotNull
    private UUID id;

    @NotNull
    private UUID workoutGroupId;

    @NotNull
    private SimpleUserDTO user;

    @Size(max = 20)
    @NotNull
    private String status;

    @NotNull
    private Instant createdAt;
}
