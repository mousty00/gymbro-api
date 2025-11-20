package com.mousty.gymbro.service;

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
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class WorkoutHistoryService extends GenericService<WorkoutHistory, WorkoutHistoryDTO, WorkoutHistoryMapper, WorkoutHistoryRepository> {

    private final WorkoutHistoryMapper mapper;
    private final WorkoutHistoryRepository repository;

    public WorkoutHistoryService(final WorkoutHistoryMapper mapper, final WorkoutHistoryRepository repository) {
        super(mapper, repository);
        this.mapper = mapper;
        this.repository = repository;
    }

    public Connection<WorkoutHistoryDTO> getAllWorkoutHistories(Pageable pageable) {
        return getAll(pageable);
    }

    public Connection<WorkoutHistoryDTO> getUserWorkoutHistories(String username, Pageable pageable) {
        final Page<WorkoutHistory> page = repository.findAllByUser_Username(username, pageable);
        final List<WorkoutHistoryDTO> listDTO = page.map(mapper::toDTO).toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());
        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    public Connection<WorkoutHistoryDTO> getGroupWorkoutHistories(UUID groupId, Pageable pageable) {
        final Page<WorkoutHistory> page = repository.findAllByGroup_Id(groupId, pageable);
        final List<WorkoutHistoryDTO> listDTO = page.map(mapper::toDTO).toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());
        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    public WorkoutHistoryDTO getWorkoutHistoryById(UUID id) {
        return getById(id, "Workout history not found");
    }

    @Transactional
    public MessageResponse deleteWorkoutHistory(UUID id, String username) {
        WorkoutHistory history = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Workout history not found"));

        if (!history.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You cannot delete this workout history");
        }

        return delete(id, "Workout history not found", "Workout history deleted successfully!");
    }

    @Transactional
    public EntityResponse<WorkoutHistoryDTO> createWorkoutHistory(WorkoutHistoryInput input, String username) {
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
                .orElseThrow(() -> new NoSuchElementException("Workout history not found"));

        if (!history.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You cannot update this workout history");
        }

        repository.save(mapper.toUpdateEntity(input, history));
        return MessageResponse.builder()
                .message("Workout history updated successfully!")
                .timestamp(Instant.now())
                .build();
    }
}