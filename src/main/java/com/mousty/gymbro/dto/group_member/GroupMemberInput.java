package com.mousty.gymbro.dto.group_member;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record GroupMemberInput(
    @NotNull
    UUID groupId,

    @NotNull
    String invitedUsername
) {}
