package com.mousty.gymbro.dto.post;

import com.mousty.gymbro.dto.post_comment.SimpleCommentDTO;
import com.mousty.gymbro.dto.post_like.SimpleLikeDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {

    private UUID id;

    @NotNull
    private UserDTO user;

    @NotNull
    private String content;

    @Size(max = 255)
    private String image;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAt;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant updatedAt;

    private List<SimpleCommentDTO> comments;

    private List<SimpleLikeDTO> likes;

    private int numLikes;

    private int numComments;
}
