package com.mousty.gymbro.mapper;

import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.entity.Exercise;
import com.mousty.gymbro.dto.exercise.SimpleExerciseDTO;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.entity.WorkoutExercise;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseInput;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        builder = @Builder(disableBuilder = true)
)
public interface WorkoutExerciseMapper extends GenericMapper<WorkoutExercise, WorkoutExerciseDTO> {

    @Mapping(target = "workoutId", source = "workout.id")
    @Mapping(target = "exerciseId", source = "exercise.id")
    @Mapping(target = "exerciseName", source = "exercise.name")
    @Mapping(target = "position", expression = "java(workoutExercise.getPosition() != null ? workoutExercise.getPosition() : 0)")
    @Mapping(target = "restSeconds", expression = "java(workoutExercise.getRestSeconds() != null ? workoutExercise.getRestSeconds() : 0)")
    WorkoutExerciseDTO toDTO(WorkoutExercise workoutExercise);

    @Mapping(target = "workout", ignore = true)
    @Mapping(target = "exercise", ignore = true)
    WorkoutExercise toEntity(WorkoutExerciseDTO workoutExerciseDTO);

    @Mapping(target = "id", source = "request.id")
    @Mapping(target = "workout", source = "workout")
    @Mapping(target = "exercise", expression = "java(buildExerciseFromSimpleDTO(exerciseDTO))")
    @Mapping(target = "reps", source = "request.reps")
    @Mapping(target = "sets", source = "request.sets")
    @Mapping(target = "weight", source = "request.weight")
    @Mapping(target = "position", source = "request.position")
    @Mapping(target = "restSeconds", source = "request.restSeconds")
    WorkoutExercise toNewEntity(WorkoutExerciseInput request, Workout workout, SimpleExerciseDTO exerciseDTO);

    default Exercise buildExerciseFromSimpleDTO(SimpleExerciseDTO exerciseDTO) {
        if (exerciseDTO == null) {
            return null;
        }

        User createdBy = null;
        createdBy = User.builder()
                .id(exerciseDTO.getCreatedBy().getId())
                .build();

        return Exercise.builder()
                .id(exerciseDTO.getId())
                .name(exerciseDTO.getName())
                .description(exerciseDTO.getDescription())
                .muscleGroup(exerciseDTO.getMuscleGroup())
                .createdBy(createdBy)
                .isPublic(exerciseDTO.getIsPublic())
                .workoutExercises(null)
                .build();
    }
}