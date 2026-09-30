package com.mousty.gymbro.dto.workout;

import com.mousty.gymbro.dto.user.SimpleUserDTO;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record SimpleWorkoutDTO(
    @NotNull
    UUID id,

    @NotNull
    SimpleUserDTO user,

    @Size(max = 100)
    @NotNull
    String name,

    String description,

    @NotNull
    Boolean isPublic,

    @ColumnDefault("'{}'")
    @Column(name = "day_of_week")
    List<Integer> dayOfWeek
) {}
