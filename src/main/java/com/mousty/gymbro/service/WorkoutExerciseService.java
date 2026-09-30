package com.mousty.gymbro.service;

import com.mousty.gymbro.exception.WorkoutExerciseException;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.mapper.ExerciseMapper;
import com.mousty.gymbro.dto.exercise.SimpleExerciseDTO;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.entity.WorkoutExercise;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.mapper.WorkoutExerciseMapper;
import com.mousty.gymbro.repository.WorkoutExerciseRepository;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkoutExerciseService extends GenericService<WorkoutExercise, WorkoutExerciseDTO, WorkoutExerciseMapper, WorkoutExerciseRepository> {

    private final WorkoutService workoutService;
    private final ExerciseService exerciseService;
    private final ExerciseMapper exerciseMapper;

    public WorkoutExerciseService(
            final WorkoutExerciseMapper mapper,
            final WorkoutExerciseRepository repository,
            final WorkoutService workoutService,
            final ExerciseService exerciseService,
            final ExerciseMapper exerciseMapper) {
        super(mapper, repository);
        this.workoutService = workoutService;
        this.exerciseService = exerciseService;
        this.exerciseMapper = exerciseMapper;
    }

    /** Only rows belonging to the caller's own workouts. */
    public Connection<WorkoutExerciseDTO> getAllWorkoutExercises(Pageable pageable, String username) {
        final Page<WorkoutExercise> page = repository.findAllByWorkout_User_Username(username, pageable);
        final List<WorkoutExerciseDTO> listDTO = page.map(mapper::toDTO).toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());
        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    @Transactional
    public MessageResponse deleteWorkoutExerciseById(UUID id, String username) {
        repository.delete(getOwnedWorkoutExercise(id, username));
        return MessageResponse.builder()
                .message("Workout exercise deleted successfully!")
                .timestamp(Instant.now())
                .build();
    }

    public WorkoutExerciseDTO getWorkoutExerciseById(UUID id, String username) {
        final WorkoutExercise workoutExercise = getWorkoutExerciseEntityById(id);
        workoutService.getVisibleWorkout(workoutExercise.getWorkout().getId(), username);
        return mapper.toDTO(workoutExercise);
    }

    @Transactional
    public EntityResponse<WorkoutExerciseDTO> createWorkoutExercise(WorkoutExerciseInput request, String username) {
        // id forced to null: a client-chosen id would make save() overwrite an existing row
        final WorkoutExercise workoutExercise = repository.save(
                toEntity(request.toBuilder().id(null).build(), username));
        return EntityResponse.<WorkoutExerciseDTO>builder()
                .message("Workout exercise added successfully!")
                .timestamp(Instant.now())
                .result(mapper.toDTO(workoutExercise))
                .build();
    }

    @Transactional
    public MessageResponse updateWorkoutExercise(WorkoutExerciseInput request, String username) {
        if (request.id() == null) {
            throw WorkoutExerciseException.idRequired();
        }
        getOwnedWorkoutExercise(request.id(), username);
        repository.save(toEntity(request, username));
        return MessageResponse.builder()
                .message("Workout exercise updated successfully!")
                .timestamp(Instant.now())
                .build();
    }

    // Target workout must be the caller's; the exercise must be public or the caller's.
    private WorkoutExercise toEntity(WorkoutExerciseInput request, String username) {
        final Workout workout = workoutService.getOwnedWorkout(request.workoutId(), username);
        final SimpleExerciseDTO exercise = exerciseMapper.toSimpleDTO(
                exerciseService.getExerciseById(request.exerciseId(), username));
        return mapper.toNewEntity(request, workout, exercise);
    }

    private WorkoutExercise getOwnedWorkoutExercise(UUID id, String username) {
        final WorkoutExercise workoutExercise = getWorkoutExerciseEntityById(id);
        workoutService.getOwnedWorkout(workoutExercise.getWorkout().getId(), username);
        return workoutExercise;
    }

    private WorkoutExercise getWorkoutExerciseEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> WorkoutExerciseException.notFound(id));
    }
}
