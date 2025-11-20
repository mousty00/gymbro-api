package com.mousty.gymbro.service;

import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.mapper.WorkoutMapper;
import com.mousty.gymbro.dto.workout.WorkoutDTO;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.mapper.WorkoutGroupMapper;
import com.mousty.gymbro.repository.WorkoutGroupRepository;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupDTO;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class WorkoutGroupService extends GenericService<WorkoutGroup, WorkoutGroupDTO, WorkoutGroupMapper, WorkoutGroupRepository> {
    private final WorkoutGroupRepository repository;
    private final WorkoutGroupMapper mapper;
    private final AuthService authService;
    private final WorkoutService workoutService;
    private final UserService userService;
    private final WorkoutMapper workoutMapper;

    public WorkoutGroupService(final WorkoutGroupMapper mapper, 
                             final WorkoutGroupRepository repository, 
                             final AuthService authService, 
                             final WorkoutService workoutService, 
                             final UserService userService,
                             final WorkoutMapper workoutMapper) {
        super(mapper, repository);
        this.repository = repository;
        this.mapper = mapper;
        this.authService = authService;
        this.workoutService = workoutService;
        this.userService = userService;
        this.workoutMapper = workoutMapper;
    }

    public EntityResponse<WorkoutGroupDTO> createWorkoutGroup(WorkoutGroupInput request, String username) {
        userService.getUserById(request.getCreatedById());
        authService.checkAuthorization(request.getCreatedById(), username, "User not authorized to create workout group");
        final WorkoutDTO workoutDTO = workoutService.getWorkoutById(request.getWorkoutId());
        final Workout workout = workoutMapper.toEntity(workoutDTO);
        
        final WorkoutGroup workoutGroup = repository.save(mapper.toNewEntity(request, workout, null));
        final WorkoutGroupDTO dto = mapper.toDTO(workoutGroup);
        return EntityResponse.<WorkoutGroupDTO>builder()
                .result(dto)
                .message("Workout group created successfully!")
                .timestamp(Instant.now())
                .build();
    }

    public Connection<WorkoutGroupDTO> getAllWorkoutGroups(Pageable pageable) {
        return getAll(pageable);
    }

    public WorkoutGroupDTO getWorkoutGroupById(UUID id) {
        return getById(id, "Workout group not found");
    }



    public WorkoutGroup getWorkoutGroupEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workout group not found"));
    }

    public MessageResponse deleteWorkoutGroup(final UUID id,
                                                              final String username) {
        final WorkoutGroup workoutGroup = getWorkoutGroupEntityById(id);
        authService.checkAuthorization(workoutGroup.getCreatedBy().getId(), username, "User not authorized to create workout group");
        repository.delete(workoutGroup);
        return MessageResponse.builder()
                        .message("Workout group deleted successfully!")
                        .timestamp(Instant.now())
                .build();
    }

}