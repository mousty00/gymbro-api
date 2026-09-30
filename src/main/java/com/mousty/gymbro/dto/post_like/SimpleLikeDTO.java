package com.mousty.gymbro.dto.post_like;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record SimpleLikeDTO(
    @NotNull
    UUID id,

    @NotNull
    String username,

    @NotNull
    Instant createdAt
) {}
