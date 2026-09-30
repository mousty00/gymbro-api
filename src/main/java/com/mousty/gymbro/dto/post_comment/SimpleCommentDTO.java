package com.mousty.gymbro.dto.post_comment;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record SimpleCommentDTO(
    UUID id,

    @NotNull
    UUID postId,

    @NotNull
    String username,

    @NotNull
    String content,

    @NotNull
    Instant createdAt
) {}
