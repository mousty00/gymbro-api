package com.mousty.gymbro.dto.post_like;

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
public class SimpleLikeDTO {
    @NotNull
    private UUID id;

    @NotNull
    private String username;

    @NotNull
    private Instant createdAt;
}
