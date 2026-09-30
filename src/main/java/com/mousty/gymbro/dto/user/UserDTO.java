package com.mousty.gymbro.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder(toBuilder = true)
public record UserDTO(
    @NotNull
    UUID id,
    @NotBlank(message = "Username is required")
    String username,
    @NotBlank(message = "email is required")
    String email,
    @NotBlank(message = "firstName is required")
    String firstName,
    @NotBlank(message = "lastName is required")
    String lastName,
    @NotNull(message = "Birth date is required")
    LocalDate birthDate,
    String image,
    @NotNull
    String roleName,
    Boolean isAccountVerified,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant createdAt
) {}
