package com.mousty.gymbro.dto.workout;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutInput {

    private UUID id;

    @NotNull
    private UUID userId;

    @Size(max = 100)
    @NotNull
    private String name;

    private String description;

    @NotNull
    private Boolean isPublic = false;

    @ColumnDefault("'{}'")
    @Column(name = "day_of_week")
    private List<Integer> dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant createdAt;
}
