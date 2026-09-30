package com.mousty.gymbro.dto.exercise;

import com.mousty.gymbro.validation.Put;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record ExerciseInput(
    @NotNull(groups = {Put.class, Default.class})
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
