package com.mousty.gymbro.dto.user;

import lombok.Builder;
import java.util.UUID;

@Builder
public record SimpleUserDTO(
    UUID id,
    String username,
    String image,
    Boolean isAccountVerified
) {}
