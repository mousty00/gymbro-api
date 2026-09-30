package com.mousty.gymbro.response;

import lombok.Builder;

@Builder
public record TokenResponse(
        String message,
        String token,
        String refreshToken
) { }
