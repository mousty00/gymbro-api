package com.mousty.gymbro.dto.exercise;

import com.mousty.gymbro.dto.user.UserDTO;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record SimpleExerciseDTO(
    @NotNull
    UUID id,

    @Size(max = 100)
    @NotNull
    String name,

    String description,

    @Size(max = 50)
    @NotNull
    String muscleGroup,

    @NotNull
    Boolean isPublic,

    @NotNull
    UserDTO createdBy
) {}
