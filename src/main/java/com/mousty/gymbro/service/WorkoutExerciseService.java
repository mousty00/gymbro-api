package com.mousty.gymbro.service;

import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.mapper.ExerciseMapper;
import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.SimpleExerciseDTO;
import com.mousty.gymbro.mapper.WorkoutMapper;
import com.mousty.gymbro.dto.workout.WorkoutDTO;
import com.mousty.gymbro.entity.WorkoutExercise;
import com.mousty.gymbro.mapper.WorkoutExerciseMapper;
import com.mousty.gymbro.repository.WorkoutExerciseRepository;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WorkoutExerciseService extends GenericService<WorkoutExercise, WorkoutExerciseDTO, WorkoutExerciseMapper, WorkoutExerciseRepository> {

    private final WorkoutExerciseMapper mapper;
    private final WorkoutExerciseRepository repository;
    private final WorkoutService workoutService;
    private final ExerciseService exerciseService;
    private final WorkoutMapper workoutMapper;
    private final ExerciseMapper exerciseMapper;

    public WorkoutExerciseService(
            final WorkoutExerciseMapper mapper,
            final WorkoutExerciseRepository repository,
            final WorkoutService workoutService,
            final ExerciseService exerciseService,
            final WorkoutMapper workoutMapper,
            final ExerciseMapper exerciseMapper) {
        super(mapper, repository);
        this.mapper = mapper;
        this.repository = repository;
        this.workoutService = workoutService;
        this.exerciseService = exerciseService;
        this.workoutMapper = workoutMapper;
        this.exerciseMapper = exerciseMapper;
    }

    public Connection<WorkoutExerciseDTO> getAllWorkoutExercises(Pageable pageable) {
        return getAll(pageable);
    }

    @Transactional
    public MessageResponse deleteWorkoutExerciseById(UUID id) {
        return delete(id, "Workout exercise not found", "Workout exercise deleted successfully!");
    }

    public WorkoutExerciseDTO getWorkoutExerciseById(UUID id) {
        return getById(id, "Workout exercise not found");
    }

    @Transactional
    public ResponseEntity<EntityResponse<WorkoutExerciseDTO>> createWorkoutExercise(WorkoutExerciseInput request) {
        final WorkoutDTO workoutDTO = workoutService.getWorkoutById(request.getWorkoutId());
        final ExerciseDTO exerciseDTO = exerciseService.getExerciseById(request.getExerciseId());
        final SimpleExerciseDTO simpleExerciseDTO = exerciseMapper.toSimpleDTO(exerciseDTO);
        final WorkoutExercise newWorkoutExercise = mapper.toNewEntity(request, workoutMapper.toEntity(workoutDTO), simpleExerciseDTO);
        final WorkoutExercise workoutExercise = repository.save(newWorkoutExercise);
        return ResponseEntity.ok(EntityResponse.<WorkoutExerciseDTO>builder()
                        .message("Workout exercise added successfully!")
                        .timestamp(Instant.now())
                        .result(mapper.toDTO(workoutExercise))
                .build());
    }

    @Transactional
    public ResponseEntity<MessageResponse> updateWorkoutExercise(WorkoutExerciseInput request) {
        if(request.getId() == null) {
            throw new IllegalArgumentException("Workout exercise id is required");
        }
        final WorkoutExerciseDTO dto  = getWorkoutExerciseById(request.getId());
        workoutService.getWorkoutById(request.getWorkoutId());
        exerciseService.getExerciseById(request.getExerciseId());
        repository.save(mapper.toEntity(dto));
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Workout exercise updated successfully!")
                .timestamp(Instant.now())
                .build());
    }
}
