package com.mousty.gymbro.service;

import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.mapper.WorkoutMapper;
import com.mousty.gymbro.repository.WorkoutRepository;
import com.mousty.gymbro.dto.workout.WorkoutDTO;
import com.mousty.gymbro.dto.workout.WorkoutInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class WorkoutService extends GenericService<Workout, WorkoutDTO, WorkoutMapper, WorkoutRepository> {

    private final WorkoutMapper mapper;
    private final WorkoutRepository repository;

    public WorkoutService(final WorkoutMapper mapper, final WorkoutRepository repository, final WorkoutMapper mapper1, final WorkoutRepository repository1) {
        super(mapper, repository);
        this.mapper = mapper1;
        this.repository = repository1;
    }

    public Connection<WorkoutDTO> getAllWorkouts(Pageable pageable) {
        return getAll(pageable);
    }

    public WorkoutDTO getWorkoutById(UUID id) {
        return getById(id, "Workout not found");
    }

    @Transactional
    public MessageResponse deleteWorkoutById(UUID id) {
        return delete(id, "Workout not found", "Workout deleted successfully!");
    }

    @Transactional
    public MessageResponse updateWorkout(WorkoutInput request) {
        if (repository.findById(request.getId()).isEmpty() || request.getId() == null) {
            throw new IllegalArgumentException("Workout id is required");
        }
        final Workout workout = repository.findById(request.getId())
                .orElseThrow(() -> new NoSuchElementException("Workout not found"));
        repository.save(mapper.toUpdateEntity(request, workout));
        return MessageResponse.builder()
                .message("Workout updated successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public EntityResponse<WorkoutDTO> createWorkout(WorkoutInput request) {
        repository.save(mapper.toNewEntity(request));
        return EntityResponse.<WorkoutDTO>builder()
                .message("Workout added successfully!")
                .timestamp(Instant.now())
                .build();
    }

    public Workout getUserWorkoutEntityById(UUID id){
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("user not found"));
    }


    public List<WorkoutDTO> getWorkouts(final String username) {

        return repository.findAllByUser_Username(username)
                .stream().map(mapper::toDTO).toList();
    }
}
