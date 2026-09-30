package com.mousty.gymbro.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record UserInput(
    UUID id,
    @Size(max = 50)
    String username,
    @Size(max = 100)
    String email,
    @Size(max = 50)
    String firstName,
    @Size(max = 50)
    String lastName,
    LocalDate birthDate,
    String image
) {}
