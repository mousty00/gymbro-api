package com.mousty.gymbro.service;

import com.mousty.gymbro.exception.WorkoutException;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.mapper.WorkoutMapper;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.repository.WorkoutRepository;
import com.mousty.gymbro.dto.workout.WorkoutDTO;
import com.mousty.gymbro.dto.workout.WorkoutInput;
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

@Transactional(readOnly = true)
@Service
public class WorkoutService extends GenericService<Workout, WorkoutDTO, WorkoutMapper, WorkoutRepository> {

    private final UserService userService;

    public WorkoutService(final WorkoutMapper mapper, final WorkoutRepository repository, final UserService userService) {
        super(mapper, repository);
        this.userService = userService;
    }

    /** Public workouts plus the caller's own. */
    public Connection<WorkoutDTO> getAllWorkouts(Pageable pageable, String username) {
        final Page<Workout> page = repository.findAllByIsPublicTrueOrUser_Username(username, pageable);
        final List<WorkoutDTO> listDTO = page.map(mapper::toDTO).toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());
        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    public WorkoutDTO getWorkoutById(UUID id, String username) {
        return mapper.toDTO(getVisibleWorkout(id, username));
    }

    @Transactional
    public MessageResponse deleteWorkoutById(UUID id, String username) {
        repository.delete(getOwnedWorkout(id, username));
        return MessageResponse.builder()
                .message("Workout deleted successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public MessageResponse updateWorkout(WorkoutInput request, String username) {
        if (request.id() == null) {
            throw WorkoutException.idRequired();
        }
        final Workout workout = getOwnedWorkout(request.id(), username);
        repository.save(mapper.toUpdateEntity(request, workout));
        return MessageResponse.builder()
                .message("Workout updated successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public EntityResponse<WorkoutDTO> createWorkout(WorkoutInput request, String username) {
        final Workout workout = mapper.toNewEntity(request);
        workout.setUser(userService.getUserEntityByUsername(username));
        final Workout saved = repository.save(workout);
        return EntityResponse.<WorkoutDTO>builder()
                .message("Workout added successfully!")
                .result(mapper.toDTO(saved))
                .timestamp(Instant.now())
                .build();
    }

    public Workout getUserWorkoutEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> WorkoutException.notFound(id));
    }

    /** The workout, only if the caller owns it. Someone else's workout reads as not found. */
    public Workout getOwnedWorkout(UUID id, String username) {
        final Workout workout = getUserWorkoutEntityById(id);
        if (username == null || !workout.getUser().getUsername().equals(username)) {
            throw WorkoutException.notFound(id);
        }
        return workout;
    }

    /** The workout, if it is public or owned by the caller. */
    public Workout getVisibleWorkout(UUID id, String username) {
        final Workout workout = getUserWorkoutEntityById(id);
        if (!Boolean.TRUE.equals(workout.getIsPublic())
                && (username == null || !workout.getUser().getUsername().equals(username))) {
            throw WorkoutException.notFound(id);
        }
        return workout;
    }

    public List<WorkoutDTO> getWorkouts(final String username) {
        return repository.findAllByUser_Username(username)
                .stream().map(mapper::toDTO).toList();
    }
}
