package com.mousty.gymbro.dto.post_like;

import com.mousty.gymbro.dto.user.UserDTO;
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
public class LikeDTO {
    @NotNull
    private UUID id;

    @NotNull
    private UUID postId;

    @NotNull
    private UserDTO user;

    @NotNull
    private Instant createdAt;
}
