package com.mousty.gymbro.dto.post;

import com.mousty.gymbro.dto.post_comment.SimpleCommentDTO;
import com.mousty.gymbro.dto.post_like.SimpleLikeDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record PostDTO(
    UUID id,

    @NotNull
    UserDTO user,

    @NotNull
    String content,

    @Size(max = 255)
    String image,

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant createdAt,

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant updatedAt,

    List<SimpleCommentDTO> comments,

    List<SimpleLikeDTO> likes,

    int numLikes,

    int numComments
) {}
