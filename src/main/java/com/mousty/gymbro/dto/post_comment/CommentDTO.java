package com.mousty.gymbro.dto.post_comment;

import com.mousty.gymbro.dto.user.UserDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record CommentDTO(
    UUID id,

    @NotNull
    UUID postId,

    @NotNull
    UserDTO user,

    @NotNull
    String content,

    @NotNull
    Instant createdAt
) {}
