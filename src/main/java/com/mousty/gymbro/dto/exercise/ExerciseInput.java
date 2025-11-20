package com.mousty.gymbro.dto.exercise;

import com.mousty.gymbro.validation.Put;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseInput {

    @NotNull(groups = {Put.class, Default.class})
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
    private UUID createdByUserId;

}
