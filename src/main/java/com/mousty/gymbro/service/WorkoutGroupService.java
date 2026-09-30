package com.mousty.gymbro.service;

import com.mousty.gymbro.exception.WorkoutGroupException;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.mapper.WorkoutGroupMapper;
import com.mousty.gymbro.repository.GroupMemberRepository;
import com.mousty.gymbro.repository.WorkoutGroupRepository;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupDTO;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkoutGroupService extends GenericService<WorkoutGroup, WorkoutGroupDTO, WorkoutGroupMapper, WorkoutGroupRepository> {
    private final AuthService authService;
    private final WorkoutService workoutService;
    private final UserService userService;
    private final GroupMemberRepository groupMemberRepository;

    public WorkoutGroupService(final WorkoutGroupMapper mapper,
                             final WorkoutGroupRepository repository,
                             final AuthService authService,
                             final WorkoutService workoutService,
                             final UserService userService,
                             final GroupMemberRepository groupMemberRepository) {
        super(mapper, repository);
        this.authService = authService;
        this.workoutService = workoutService;
        this.userService = userService;
        this.groupMemberRepository = groupMemberRepository;
    }

    public EntityResponse<WorkoutGroupDTO> createWorkoutGroup(WorkoutGroupInput request, String username) {
        final Workout workout = workoutService.getVisibleWorkout(request.workoutId(), username);
        final WorkoutGroup newGroup = mapper.toNewEntity(request, workout, null);
        newGroup.setCreatedBy(userService.getUserEntityByUsername(username));
        final WorkoutGroup workoutGroup = repository.save(newGroup);
        final WorkoutGroupDTO dto = mapper.toDTO(workoutGroup);
        return EntityResponse.<WorkoutGroupDTO>builder()
                .result(dto)
                .message("Workout group created successfully!")
                .timestamp(Instant.now())
                .build();
    }

    public Connection<WorkoutGroupDTO> getAllWorkoutGroups(Pageable pageable, String username) {
        final Page<WorkoutGroup> page = repository.findAllVisibleTo(username, pageable);
        final List<WorkoutGroupDTO> listDTO = page.map(mapper::toDTO).toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());
        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    public WorkoutGroupDTO getWorkoutGroupById(UUID id, String username) {
        return mapper.toDTO(getMemberGroup(id, username));
    }

    /** The group, only if the caller created it or is an accepted member. Otherwise not found. */
    public WorkoutGroup getMemberGroup(UUID id, String username) {
        final WorkoutGroup group = getWorkoutGroupEntityById(id);
        if (!isMember(group, username)) {
            throw WorkoutGroupException.notFound(id);
        }
        return group;
    }

    public boolean isMember(WorkoutGroup group, String username) {
        return username != null && (group.getCreatedBy().getUsername().equals(username)
                || groupMemberRepository.existsByUser_UsernameAndGroup_IdAndStatus(username, group.getId(), "accepted"));
    }

    public WorkoutGroup getWorkoutGroupEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> WorkoutGroupException.notFound(id));
    }

    public MessageResponse deleteWorkoutGroup(final UUID id, final String username) {
        final WorkoutGroup workoutGroup = getWorkoutGroupEntityById(id);
        authService.checkAuthorization(workoutGroup.getCreatedBy(), username, "User not authorized to delete workout group");
        repository.delete(workoutGroup);
        return MessageResponse.builder()
                        .message("Workout group deleted successfully!")
                        .timestamp(Instant.now())
                .build();
    }
}
