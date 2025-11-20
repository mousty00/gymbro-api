package com.mousty.gymbro.mapper;

import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.entity.GroupMember;
import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.service.WorkoutService;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupDTO;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupInput;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class WorkoutGroupMapper implements GenericMapper<WorkoutGroup, WorkoutGroupDTO> {
    private final WorkoutMapper workoutMapper;
    private final UserMapper userMapper;
    private final WorkoutService workoutService;
    private final UserService userService;

    public WorkoutGroupMapper(@Lazy final WorkoutMapper workoutMapper,
                              final UserMapper userMapper,
                              @Lazy final WorkoutService workoutService,
                              final UserService userService) {
        this.workoutMapper = workoutMapper;
        this.userMapper = userMapper;
        this.workoutService = workoutService;
        this.userService = userService;
    }

    @Override
    public WorkoutGroupDTO toDTO(final WorkoutGroup workoutGroup) {
        return WorkoutGroupDTO.builder()
                .id(workoutGroup.getId())
                .name(workoutGroup.getName())
                .workout(workoutMapper.toSimpleDTO(workoutGroup.getWorkout()))
                .createdBy(userMapper.toSimpleDTO(workoutGroup.getCreatedBy(), userService.generateImageUrl(workoutGroup.getCreatedBy())))
                .status(workoutGroup.getStatus())
                .scheduledFor(workoutGroup.getScheduledFor())
                .createdAt(workoutGroup.getCreatedAt())
                .groupMembers(workoutGroup.getGroupMembers() != null ?
                        workoutGroup.getGroupMembers().stream()
                                .map(this::mapGroupMemberToDTO)
                                .collect(toList()) : null)
                .build();
    }

    private GroupMemberDTO mapGroupMemberToDTO(GroupMember groupMember) {
        return GroupMemberDTO.builder()
                .id(groupMember.getId())
                .workoutGroupId(groupMember.getGroup().getId())
                .user(userMapper.toSimpleDTO(groupMember.getUser(), userService.generateImageUrl(groupMember.getUser())))
                .createdAt(groupMember.getCreatedAt())
                .status(groupMember.getStatus())
                .build();
    }

    @Override
    public WorkoutGroup toEntity(final WorkoutGroupDTO dto) {
        return WorkoutGroup.builder()
                .id(dto.getId())
                .name(dto.getName())
                .status(dto.getStatus())
                .scheduledFor(dto.getScheduledFor())
                .workout(workoutService.getUserWorkoutEntityById(dto.getWorkout().getId()))
                .createdAt(dto.getCreatedAt())
                .workoutHistories(null)
                .build();
    }

    public WorkoutGroup toNewEntity(final WorkoutGroupInput request,
                                    final Workout workout,
                                    List<GroupMember> groupMembers) {
        return WorkoutGroup.builder()
                .name(request.getName())
                .status("scheduled")
                .scheduledFor(request.getScheduledFor())
                .workout(workout)
                .groupMembers(groupMembers)
                .build();
    }
}