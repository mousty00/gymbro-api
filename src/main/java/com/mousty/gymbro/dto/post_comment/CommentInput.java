package com.mousty.gymbro.dto.post_comment;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record CommentInput(
    @NotNull(message="post id is required")
    UUID postId,

    @NotNull(message="content is required")
    String content
) {}
