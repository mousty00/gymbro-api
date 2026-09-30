package com.mousty.gymbro.dto.post_like;

import com.mousty.gymbro.dto.user.UserDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record LikeDTO(
    @NotNull
    UUID id,

    @NotNull
    UUID postId,

    @NotNull
    UserDTO user,

    @NotNull
    Instant createdAt
) {}
