package com.mousty.gymbro.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record EntityResponse<T>(
        String message,
        Instant timestamp,
        T result
) { }
