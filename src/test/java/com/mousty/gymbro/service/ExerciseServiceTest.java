package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.ExerciseInput;
import com.mousty.gymbro.entity.Exercise;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.exception.ExerciseException;
import com.mousty.gymbro.mapper.ExerciseMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.ExerciseRepository;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock private ExerciseMapper mapper;
    @Mock private ExerciseRepository repository;
    @Mock private UserService userService;
    @Mock private AuthService authService;

    private ExerciseService service;

    private final User alice = User.builder().username("alice").build();
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExerciseService(mapper, repository, userService, authService);
    }

    private Exercise exercise(boolean isPublic) {
        return Exercise.builder().id(id).name("Squat").description("legs").muscleGroup("legs")
                .isPublic(isPublic).createdBy(alice).build();
    }

    private void stored(Exercise exercise) {
        when(repository.findById(id)).thenReturn(Optional.of(exercise));
    }

    private void forbidFor(Exercise exercise, String username) {
        doThrow(AuthException.forbidden("nope"))
                .when(authService).checkAuthorization(eq(exercise.getCreatedBy()), eq(username), anyString());
    }

    @Nested
    @DisplayName("getAllExercises")
    class GetAll {

        @Test
        @DisplayName("queries public exercises plus the caller's own")
        void publicPlusOwn() {
            Pageable pageable = PageRequest.of(0, 10);
            Exercise e = exercise(true);
            ExerciseDTO dto = ExerciseDTO.builder().id(id).build();
            when(repository.findAllByIsPublicTrueOrCreatedBy_Username(null, pageable))
                    .thenReturn(new PageImpl<>(List.of(e), pageable, 1));
            when(mapper.toDTO(e)).thenReturn(dto);

            Connection<ExerciseDTO> result = service.getAllExercises(pageable, null);

            assertThat(result.results()).containsExactly(dto);
            assertThat(result.totalCount()).isEqualTo(1L);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getExerciseById")
    class GetById {

        @Test
        @DisplayName("public exercise is visible to anonymous callers")
        void publicToAnonymous() {
            Exercise e = exercise(true);
            stored(e);
            ExerciseDTO dto = ExerciseDTO.builder().id(id).build();
            when(mapper.toDTO(e)).thenReturn(dto);

            assertThat(service.getExerciseById(id, null)).isSameAs(dto);
        }

        @Test
        @DisplayName("private exercise is visible to its creator")
        void privateToCreator() {
            Exercise e = exercise(false);
            stored(e);
            ExerciseDTO dto = ExerciseDTO.builder().id(id).build();
            when(mapper.toDTO(e)).thenReturn(dto);

            assertThat(service.getExerciseById(id, "alice")).isSameAs(dto);
        }

        @Test
        @DisplayName("private exercise reads as not found for another user")
        void privateToOther() {
            stored(exercise(false));

            assertThatThrownBy(() -> service.getExerciseById(id, "bob")).isInstanceOf(ExerciseException.class);
            verify(mapper, never()).toDTO(any());
        }

        @Test
        @DisplayName("private exercise reads as not found for anonymous callers")
        void privateToAnonymous() {
            stored(exercise(false));

            assertThatThrownBy(() -> service.getExerciseById(id, null)).isInstanceOf(ExerciseException.class);
        }

        @Test
        @DisplayName("private exercise with no creator reads as not found")
        void privateWithoutCreator() {
            Exercise e = exercise(false);
            e.setCreatedBy(null);
            stored(e);

            assertThatThrownBy(() -> service.getExerciseById(id, "bob")).isInstanceOf(ExerciseException.class);
        }

        @Test
        @DisplayName("missing exercise is not found")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getExerciseById(id, "alice")).isInstanceOf(ExerciseException.class);
        }
    }

    @Nested
    @DisplayName("getExerciseEntityById")
    class GetEntityById {

        @Test
        @DisplayName("returns the stored entity")
        void found() {
            Exercise e = exercise(false);
            stored(e);

            assertThat(service.getExerciseEntityById(id)).isSameAs(e);
        }

        @Test
        @DisplayName("throws not found when missing")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getExerciseEntityById(id)).isInstanceOf(ExerciseException.class);
        }
    }

    @Nested
    @DisplayName("updateExercise")
    class Update {

        private final ExerciseInput request = ExerciseInput.builder()
                .id(id).name("Front squat").description("quads").muscleGroup("quads").isPublic(true).build();

        @Test
        @DisplayName("authorizes against the stored creator and applies the request fields")
        void owner() {
            Exercise e = exercise(false);
            stored(e);

            MessageResponse response = service.updateExercise(request, "alice");

            verify(authService).checkAuthorization(alice, "alice", "User not authorized to update exercise");
            verify(repository).save(e);
            assertThat(e.getName()).isEqualTo("Front squat");
            assertThat(e.getDescription()).isEqualTo("quads");
            assertThat(e.getMuscleGroup()).isEqualTo("quads");
            assertThat(e.getIsPublic()).isTrue();
            assertThat(e.getCreatedBy()).isSameAs(alice);
            assertThat(response.message()).isEqualTo("Exercise updated!");
        }

        @Test
        @DisplayName("forbidden caller: exception propagates and nothing is saved or modified")
        void forbidden() {
            Exercise e = exercise(false);
            stored(e);
            forbidFor(e, "bob");

            assertThatThrownBy(() -> service.updateExercise(request, "bob")).isInstanceOf(AuthException.class);
            verify(repository, never()).save(any());
            assertThat(e.getName()).isEqualTo("Squat");
        }

        @Test
        @DisplayName("missing exercise is not found and nothing is saved")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateExercise(request, "alice")).isInstanceOf(ExerciseException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteExerciseById")
    class Delete {

        @Test
        @DisplayName("authorizes against the stored creator and deletes")
        void owner() {
            Exercise e = exercise(true);
            stored(e);

            MessageResponse response = service.deleteExerciseById(id, "alice");

            verify(authService).checkAuthorization(alice, "alice", "User not authorized to delete exercise");
            verify(repository).delete(e);
            assertThat(response.message()).isEqualTo("Exercise deleted!");
        }

        @Test
        @DisplayName("forbidden caller: nothing deleted")
        void forbidden() {
            Exercise e = exercise(true);
            stored(e);
            forbidFor(e, "bob");

            assertThatThrownBy(() -> service.deleteExerciseById(id, "bob")).isInstanceOf(AuthException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("missing exercise is not found and nothing deleted")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteExerciseById(id, "alice")).isInstanceOf(ExerciseException.class);
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("createExercise")
    class Create {

        @Test
        @DisplayName("creator is the caller resolved by username")
        void creatorFromUsername() {
            ExerciseInput request = ExerciseInput.builder().name("Deadlift").muscleGroup("back").isPublic(false).build();
            Exercise mapped = exercise(false);
            ExerciseDTO dto = ExerciseDTO.builder().id(id).build();
            when(userService.getUserEntityByUsername("alice")).thenReturn(alice);
            when(mapper.toNewEntity(request, alice)).thenReturn(mapped);
            when(repository.save(mapped)).thenReturn(mapped);
            when(mapper.toDTO(mapped)).thenReturn(dto);

            EntityResponse<ExerciseDTO> response = service.createExercise(request, "alice");

            verify(mapper).toNewEntity(request, alice);
            assertThat(response.result()).isSameAs(dto);
            assertThat(response.message()).isEqualTo("Exercise added!");
        }
    }
}
