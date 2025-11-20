package com.mousty.gymbro.dto.post_comment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleCommentDTO {

    private UUID id;

    @NotNull
    private UUID postId;

    @NotNull
    private String username;

    @NotNull
    private String content;

    @NotNull
    private Instant createdAt;
}