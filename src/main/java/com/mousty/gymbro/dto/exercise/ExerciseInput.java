package com.mousty.gymbro.dto.exercise;

import com.mousty.gymbro.validation.Put;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record ExerciseInput(
    // only for updates; ignored on create (the id is generated)
    @NotNull(groups = Put.class)
    UUID id,

    @Size(max = 100)
    @NotNull
    String name,

    String description,

    @Size(max = 50)
    @NotNull
    String muscleGroup,

    @NotNull
    Boolean isPublic
) {}
