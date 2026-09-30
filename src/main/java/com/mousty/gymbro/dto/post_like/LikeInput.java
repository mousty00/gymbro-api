package com.mousty.gymbro.dto.post_like;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record LikeInput(
    @NotNull
    UUID postId,

    @NotNull
    UUID userId
) {}
