package com.mousty.gymbro.dto.post_like;

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
public class LikeInput {

    @NotNull
    private UUID postId;

    @NotNull
    private UUID userId;

}
