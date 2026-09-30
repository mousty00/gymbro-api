package com.mousty.gymbro.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginDTO(
    @NotBlank(message = "Username is required")
    String username,
    @NotBlank(message = "Password is required")
    String password
) {}
