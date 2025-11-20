package com.mousty.gymbro.dto.workout;

import com.mousty.gymbro.dto.user.SimpleUserDTO;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleWorkoutDTO {

    @NotNull
    private UUID id;

    @NotNull
    private SimpleUserDTO user;

    @Size(max = 100)
    @NotNull
    private String name;

    private String description;

    @NotNull
    private Boolean isPublic = false;

    @ColumnDefault("'{}'")
    @Column(name = "day_of_week")
    private List<Integer> dayOfWeek;
}
