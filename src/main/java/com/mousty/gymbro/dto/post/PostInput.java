package com.mousty.gymbro.dto.post;

import java.util.UUID;

public record PostInput(
        UUID id,
        UUID userId,
        String content,
        String image
) {}
