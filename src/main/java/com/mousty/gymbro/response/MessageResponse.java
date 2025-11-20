package com.mousty.gymbro.response;

import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Builder
public record MessageResponse(
        String message,
        @CreationTimestamp
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant timestamp
) { }
