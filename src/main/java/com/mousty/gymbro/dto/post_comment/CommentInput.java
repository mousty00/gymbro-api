package com.mousty.gymbro.dto.post_comment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentInput {

    @NotNull(message="post id is required")
    private UUID postId;

    @NotNull(message="user id is required")
    private UUID userId;

    @NotNull(message="content is required")
    private String content;
}