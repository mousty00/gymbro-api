package com.mousty.gymbro.dto.friendship;

import com.mousty.gymbro.dto.user.SimpleUserDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record FriendshipDTO(
    @NotNull
    UUID id,

    @NotNull
    SimpleUserDTO user,

    @NotNull
    SimpleUserDTO friend,

    @Size(max = 20)
    @NotNull
    String status,

    @NotNull
    Instant createdAt
) {}
