package com.mousty.gymbro.service;

import com.mousty.gymbro.entity.Exercise;
import com.mousty.gymbro.exception.ExerciseException;
import com.mousty.gymbro.mapper.ExerciseMapper;
import com.mousty.gymbro.repository.ExerciseRepository;
import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.ExerciseInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseMapper mapper;
    private final ExerciseRepository repository;
    private final UserService userService;
    private final AuthService authService;

    /** Public exercises plus the caller's own (anonymous callers see public only). */
    public Connection<ExerciseDTO> getAllExercises(Pageable pageable, String username) {
        final Page<Exercise> page = repository.findAllByIsPublicTrueOrCreatedBy_Username(username, pageable);
        final List<ExerciseDTO> listDTO = page
                .map(mapper::toDTO)
                .toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());

        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    public ExerciseDTO getExerciseById(UUID id, String username) {
        final Exercise exercise = getExerciseEntityById(id);
        if (!Boolean.TRUE.equals(exercise.getIsPublic())
                // createdBy is null once the creator's account is deleted (ON DELETE SET NULL)
                && (username == null || exercise.getCreatedBy() == null
                    || !exercise.getCreatedBy().getUsername().equals(username))) {
            throw ExerciseException.notFound(id);
        }
        return mapper.toDTO(exercise);
    }

    @Transactional
    public MessageResponse deleteExerciseById(UUID id, String username) {
        final Exercise exercise = getExerciseEntityById(id);
        authService.checkAuthorization(exercise.getCreatedBy(), username, "User not authorized to delete exercise");
        repository.delete(exercise);
        return MessageResponse.builder()
                .message("Exercise deleted!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public MessageResponse updateExercise(ExerciseInput request, String username) {
        final Exercise exercise = getExerciseEntityById(request.id());
        // authorize against the stored owner; ownership never changes on update
        authService.checkAuthorization(exercise.getCreatedBy(), username, "User not authorized to update exercise");
        exercise.setName(request.name());
        exercise.setDescription(request.description());
        exercise.setMuscleGroup(request.muscleGroup());
        exercise.setIsPublic(request.isPublic());
        repository.save(exercise);
        return MessageResponse.builder()
                .message("Exercise updated!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public EntityResponse<ExerciseDTO> createExercise(ExerciseInput request, String username) {
        final User owner = userService.getUserEntityByUsername(username);
        final Exercise exercise = repository.save(mapper.toNewEntity(request, owner));
        return EntityResponse.<ExerciseDTO>builder()
                .message("Exercise added!")
                .result(mapper.toDTO(exercise))
                .timestamp(Instant.now())
                .build();
    }

    public Exercise getExerciseEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ExerciseException.notFound(id));
    }
}
