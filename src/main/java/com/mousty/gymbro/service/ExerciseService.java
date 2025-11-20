package com.mousty.gymbro.service;

import com.mousty.gymbro.entity.Exercise;
import com.mousty.gymbro.mapper.ExerciseMapper;
import com.mousty.gymbro.repository.ExerciseRepository;
import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.ExerciseInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.mapper.UserMapper;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseMapper mapper;
    private final ExerciseRepository repository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthService authService;


    public Connection<ExerciseDTO> getAllExercises(Pageable pageable) {
        final Page<Exercise> page = repository.findAll(pageable);
        final List<ExerciseDTO> listDTO = page
                .map(mapper::toDTO)
                .toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());

        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    public ExerciseDTO getExerciseById(UUID id) {
        final Exercise t = getExerciseEntityById(id);
        return mapper.toDTO(t);
    }

    @Transactional
    public MessageResponse deleteExerciseById(UUID id, String username) {
        authService.checkAuthorization(id, username, "User not authorized to delete exercise");
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Exercise not found");
        }
        repository.deleteById(id);
        return MessageResponse.builder()
                .message("Exercise deleted!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public MessageResponse updateExercise(ExerciseInput request, String username) {
        authService.checkAuthorization(request.getCreatedByUserId(), username, "User not authorized to update exercise");
        final User user = userService.getUserEntityById(request.getCreatedByUserId());
        final Exercise exercise = getExerciseEntityById(request.getId());
        repository.save(mapper.toEntity(
                mapper.toDTO(exercise),
                user,
                exercise.getWorkoutExercises()));
        return MessageResponse.builder()
                .message("Exercise updated!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public ResponseEntity<EntityResponse<ExerciseDTO>> createExercise(ExerciseInput request, String username) {
        authService.checkAuthorization(request.getCreatedByUserId(), username, "User not authorized to create exercise");
        final UserDTO userDTO = userService.getUserById(request.getCreatedByUserId());
        final Exercise exercise = repository.save(mapper.toNewEntity(request, userMapper.toEntity(userDTO)));
        return ResponseEntity.ok(EntityResponse.<ExerciseDTO>builder()
                .message("Exercise added!")
                .result(mapper.toDTO(exercise))
                .timestamp(Instant.now())
                .build());
    }

    public Exercise getExerciseEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Exercise not found"));
    }
}

