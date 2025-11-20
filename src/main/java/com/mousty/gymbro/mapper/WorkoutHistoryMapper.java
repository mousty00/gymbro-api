package com.mousty.gymbro.mapper;

import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.entity.WorkoutHistory;
import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.service.WorkoutService;
import com.mousty.gymbro.service.WorkoutGroupService;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryDTO;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkoutHistoryMapper implements GenericMapper<WorkoutHistory, WorkoutHistoryDTO> {
    
    private final UserService userService;
    private final WorkoutService workoutService;
    private final WorkoutGroupService workoutGroupService;

    @Override
    public WorkoutHistoryDTO toDTO(WorkoutHistory entity) {
        return WorkoutHistoryDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .workoutId(entity.getWorkout().getId())
                .groupId(entity.getGroup() != null ? entity.getGroup().getId() : null)
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .notes(entity.getNotes())
                .build();
    }

    @Override
    public WorkoutHistory toEntity(final WorkoutHistoryDTO workoutHistoryDTO) {
        return null;
    }

    public WorkoutHistory toNewEntity(WorkoutHistoryInput input, String username) {
        WorkoutHistory entity = new WorkoutHistory();
        entity.setUser(userService.getUserEntityByUsername(username));
        entity.setWorkout(workoutService.getUserWorkoutEntityById(input.getWorkoutId()));
        if (input.getGroupId() != null) {
            entity.setGroup(workoutGroupService.getWorkoutGroupEntityById(input.getGroupId()));
        }
        entity.setStartedAt(input.getStartedAt());
        entity.setCompletedAt(input.getCompletedAt());
        entity.setNotes(input.getNotes());
        return entity;
    }

    public WorkoutHistory toUpdateEntity(WorkoutHistoryInput input, WorkoutHistory entity) {
        entity.setCompletedAt(input.getCompletedAt());
        entity.setNotes(input.getNotes());
        return entity;
    }
}