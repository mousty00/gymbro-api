package com.mousty.gymbro.request;

import jakarta.validation.constraints.NotBlank;

public record OTPRequest(
        @NotBlank
        String username,
        @NotBlank
        String otp
) { }
