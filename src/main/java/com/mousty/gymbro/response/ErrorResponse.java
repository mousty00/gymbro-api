package com.mousty.gymbro.response;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder(toBuilder = true)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    Map<String, String> details
) {}
