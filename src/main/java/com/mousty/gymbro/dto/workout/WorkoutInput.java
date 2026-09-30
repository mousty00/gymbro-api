package com.mousty.gymbro.dto.workout;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record WorkoutInput(
    UUID id,

    @Size(max = 100)
    @NotNull
    String name,

    String description,

    @NotNull
    Boolean isPublic,

    @ColumnDefault("'{}'")
    @Column(name = "day_of_week")
    List<Integer> dayOfWeek,

    @Column(name = "start_time")
    LocalTime startTime
) {}
