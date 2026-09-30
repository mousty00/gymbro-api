package com.mousty.gymbro.dto.workout;

import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupDTO;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryDTO;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record WorkoutDTO(
    @NotNull
    UUID id,

    @NotNull
    UserDTO user,

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
    LocalTime startTime,

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant createdAt,

    List<WorkoutExerciseDTO> workoutExercises,

    List<WorkoutGroupDTO> workoutGroups,

    @OneToMany(mappedBy = "workout")
    List<WorkoutHistoryDTO> workoutHistories
) {}
