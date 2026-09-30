package com.mousty.gymbro.service;

import com.mousty.gymbro.exception.WorkoutHistoryException;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.entity.WorkoutHistory;
import com.mousty.gymbro.mapper.WorkoutHistoryMapper;
import com.mousty.gymbro.repository.WorkoutHistoryRepository;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryDTO;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
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
public class WorkoutHistoryService extends GenericService<WorkoutHistory, WorkoutHistoryDTO, WorkoutHistoryMapper, WorkoutHistoryRepository> {

    private final WorkoutService workoutService;
    private final WorkoutGroupService workoutGroupService;

    public WorkoutHistoryService(final WorkoutHistoryMapper mapper, final WorkoutHistoryRepository repository,
                                 final WorkoutService workoutService, final WorkoutGroupService workoutGroupService) {
        super(mapper, repository);
        this.workoutService = workoutService;
        this.workoutGroupService = workoutGroupService;
    }

    public Connection<WorkoutHistoryDTO> getUserWorkoutHistories(String username, Pageable pageable) {
        final Page<WorkoutHistory> page = repository.findAllByUser_Username(username, pageable);
        final List<WorkoutHistoryDTO> listDTO = page.map(mapper::toDTO).toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());
        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    public Connection<WorkoutHistoryDTO> getGroupWorkoutHistories(UUID groupId, Pageable pageable, String username) {
        workoutGroupService.getMemberGroup(groupId, username);
        final Page<WorkoutHistory> page = repository.findAllByGroup_Id(groupId, pageable);
        final List<WorkoutHistoryDTO> listDTO = page.map(mapper::toDTO).toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());
        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    /** Visible to its owner and, for a group session, to the group's members. */
    public WorkoutHistoryDTO getWorkoutHistoryById(UUID id, String username) {
        final WorkoutHistory history = repository.findById(id)
                .orElseThrow(() -> WorkoutHistoryException.notFound(id));
        final boolean owner = username != null && history.getUser().getUsername().equals(username);
        if (!owner && (history.getGroup() == null || !workoutGroupService.isMember(history.getGroup(), username))) {
            throw WorkoutHistoryException.notFound(id);
        }
        return mapper.toDTO(history);
    }

    @Transactional
    public MessageResponse deleteWorkoutHistory(UUID id, String username) {
        WorkoutHistory history = repository.findById(id)
                .orElseThrow(() -> WorkoutHistoryException.notFound(id));

        if (!history.getUser().getUsername().equals(username)) {
            throw WorkoutHistoryException.unauthorized();
        }

        repository.deleteById(id);
        return MessageResponse.builder()
                .message("Workout history deleted successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public EntityResponse<WorkoutHistoryDTO> createWorkoutHistory(WorkoutHistoryInput input, String username) {
        // you can log a session of a workout you can see, and only into a group you belong to
        workoutService.getVisibleWorkout(input.workoutId(), username);
        if (input.groupId() != null) {
            workoutGroupService.getMemberGroup(input.groupId(), username);
        }
        WorkoutHistory history = mapper.toNewEntity(input, username);
        repository.save(history);
        return EntityResponse.<WorkoutHistoryDTO>builder()
                .result(mapper.toDTO(history))
                .message("Workout history created successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public MessageResponse updateWorkoutHistory(UUID id, WorkoutHistoryInput input, String username) {
        WorkoutHistory history = repository.findById(id)
                .orElseThrow(() -> WorkoutHistoryException.notFound(id));

        if (!history.getUser().getUsername().equals(username)) {
            throw WorkoutHistoryException.unauthorized();
        }

        repository.save(mapper.toUpdateEntity(input, history));
        return MessageResponse.builder()
                .message("Workout history updated successfully!")
                .timestamp(Instant.now())
                .build();
    }
}
