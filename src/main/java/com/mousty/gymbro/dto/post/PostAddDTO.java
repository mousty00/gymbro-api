package com.mousty.gymbro.dto.post;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record PostAddDTO(
    @NotNull
    UUID userId,

    @NotNull
    String content
) {}
