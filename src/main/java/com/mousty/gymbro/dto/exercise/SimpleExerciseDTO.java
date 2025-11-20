package com.mousty.gymbro.dto.exercise;

import com.mousty.gymbro.dto.user.UserDTO;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleExerciseDTO {
    @NotNull
    private UUID id;

    @Size(max = 100)
    @NotNull
    private String name;

    private String description;

    @Size(max = 50)
    @NotNull
    private String muscleGroup;

    @NotNull
    private Boolean isPublic = false;

    @NotNull
    private UserDTO createdBy;
}