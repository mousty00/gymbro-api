package com.mousty.gymbro.mapper;

import com.mousty.gymbro.entity.Exercise;
import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.ExerciseInput;
import com.mousty.gymbro.dto.exercise.SimpleExerciseDTO;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.entity.WorkoutExercise;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        builder = @Builder(disableBuilder = true)
)
public interface ExerciseMapper {

    @Mapping(target = "createdBy", source = "exercise.createdBy")
    @Mapping(target = "workoutExercises", source = "exercise.workoutExercises", qualifiedByName = "mapWorkoutExercises")
    ExerciseDTO toDTO(Exercise exercise);

    @Named("mapWorkoutExercises")
    default List<WorkoutExerciseDTO> mapWorkoutExercises(List<WorkoutExercise> workoutExercises) {
        if (workoutExercises == null) {
            return Collections.emptyList();
        }
        return workoutExercises.stream()
                .map(this::toBasicWorkoutExerciseDTO)
                .toList();
    }

    @Mapping(target = "workoutId", source = "workout.id")
    @Mapping(target = "exerciseId", source = "exercise.id")
    @Mapping(target = "exerciseName", source = "exercise.name")

    WorkoutExerciseDTO toBasicWorkoutExerciseDTO(WorkoutExercise workoutExercise);

    @Mapping(target = "id", source = "exercise.id")
    @Mapping(target = "createdBy", source = "userDTO")
    SimpleExerciseDTO toSimpleDTO(Exercise exercise, UserDTO userDTO);

    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "createdBy", source = "user")
    @Mapping(target = "workoutExercises", source = "workoutExercises")
    Exercise toEntity(ExerciseDTO dto, User user, List<WorkoutExercise> workoutExercises);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "workoutExercises", expression = "java(java.util.Collections.emptyList())")
    Exercise toNewEntity(ExerciseInput request, User createdBy);

    // Overloaded method to convert from ExerciseDTO to SimpleExerciseDTO
    SimpleExerciseDTO toSimpleDTO(ExerciseDTO dto);
}