package com.mousty.gymbro.dto.friendship;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder(toBuilder = true)
public record FriendshipInput(
    @NotNull
    String friend
) {}
