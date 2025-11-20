package com.mousty.gymbro.mapper;

import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.dto.workout.SimpleWorkoutDTO;
import com.mousty.gymbro.dto.workout.WorkoutDTO;
import com.mousty.gymbro.dto.workout.WorkoutInput;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupDTO;
import com.mousty.gymbro.entity.WorkoutHistory;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryDTO;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class, WorkoutExerciseMapper.class}
)
public abstract class WorkoutMapper implements GenericMapper<Workout, WorkoutDTO> {

    @Autowired
    protected UserService userService;

    @Autowired
    @Lazy
    protected WorkoutGroupMapper workoutGroupMapper;

    @Autowired
    @Lazy
    protected WorkoutHistoryMapper workoutHistoryMapper;
    @Autowired
    private UserMapper userMapper;

    @Mapping(target = "workoutGroups", source = "workoutGroups", qualifiedByName = "mapWorkoutGroups")
    @Mapping(target = "workoutHistories", source = "workoutHistories", qualifiedByName = "mapWorkoutHistories")
    public abstract WorkoutDTO toDTO(Workout workout);

    @Mapping(target = "workoutGroups", source = "workoutGroups", qualifiedByName = "mapWorkoutGroupEntities")
    @Mapping(target = "workoutHistories", source = "workoutHistories", qualifiedByName = "mapWorkoutHistoryEntities")
    public abstract Workout toEntity(WorkoutDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "userId", qualifiedByName = "mapUserFromId")
    @Mapping(target = "workoutGroups", ignore = true)
    @Mapping(target = "workoutHistories", ignore = true)
    @Mapping(target = "workoutExercises", ignore = true)
    public abstract Workout toNewEntity(WorkoutInput request);

    @Mapping(target = "id", source = "workout.id")
    @Mapping(target = "user", source = "request.userId", qualifiedByName = "mapUserFromId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "isPublic", source = "request.isPublic")
    @Mapping(target = "dayOfWeek", source = "request.dayOfWeek")
    @Mapping(target = "startTime", source = "request.startTime")
    @Mapping(target = "createdAt", source = "request.createdAt")
    @Mapping(target = "workoutGroups", source = "workout.workoutGroups")
    @Mapping(target = "workoutHistories", source = "workout.workoutHistories")
    @Mapping(target = "workoutExercises", source = "workout.workoutExercises")
    public abstract Workout toUpdateEntity(WorkoutInput request, Workout workout);

    @Mapping(target = "user", source = "user", qualifiedByName = "mapUserToSimpleDTO")
    public abstract SimpleWorkoutDTO toSimpleDTO(Workout workout);

    // Helper methods
    @Named("mapUserFromId")
    protected User mapUserFromId(UUID userId) {
        if (userId == null) {
            return null;
        }
        return User.builder().id(userId).build();
    }

    @Named("mapUserToSimpleDTO")
    protected SimpleUserDTO mapUserToSimpleDTO(User user) {
        if (user == null) {
            return null;
        }
        String imageUrl = userService.generateImageUrl(user);
        return userMapper.toSimpleDTO(user, imageUrl);
    }

    @Named("mapWorkoutGroups")
    protected List<WorkoutGroupDTO>  mapWorkoutGroups(List<WorkoutGroup>  workoutGroups) {
        if (workoutGroups == null) {
            return null;
        }
        return workoutGroupMapper.toDTOList(workoutGroups);
    }

    @Named("mapWorkoutHistories")
    protected List<WorkoutHistoryDTO> mapWorkoutHistories(List<WorkoutHistory> workoutHistories) {
        if (workoutHistories == null) {
            return null;
        }
        return workoutHistoryMapper.toDTOList(workoutHistories);
    }

    @Named("mapWorkoutGroupEntities")
    protected List<WorkoutGroup> mapWorkoutGroupEntities(List<WorkoutGroupDTO> workoutGroupDTOs) {
        if (workoutGroupDTOs == null) {
            return null;
        }
        return workoutGroupMapper.toEntityList(workoutGroupDTOs);
    }

    @Named("mapWorkoutHistoryEntities")
    protected List<WorkoutHistory> mapWorkoutHistoryEntities(List<WorkoutHistoryDTO> workoutHistoryDTOs) {
        if (workoutHistoryDTOs == null) {
            return null;
        }
        return workoutHistoryMapper.toEntityList(workoutHistoryDTOs);
    }
}