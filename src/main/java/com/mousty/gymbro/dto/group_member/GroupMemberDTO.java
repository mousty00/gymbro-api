package com.mousty.gymbro.dto.group_member;

import com.mousty.gymbro.dto.user.SimpleUserDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record GroupMemberDTO(
    @NotNull
    UUID id,

    @NotNull
    UUID workoutGroupId,

    @NotNull
    SimpleUserDTO user,

    @Size(max = 20)
    @NotNull
    String status,

    @NotNull
    Instant createdAt
) {}
