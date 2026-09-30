package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.workout.WorkoutDTO;
import com.mousty.gymbro.dto.workout.WorkoutInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.exception.WorkoutException;
import com.mousty.gymbro.mapper.WorkoutMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.WorkoutRepository;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock private WorkoutMapper mapper;
    @Mock private WorkoutRepository repository;
    @Mock private UserService userService;

    private WorkoutService service;

    private final User alice = User.builder().username("alice").build();
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WorkoutService(mapper, repository, userService);
    }

    private Workout workout(boolean isPublic) {
        return Workout.builder().id(id).user(alice).name("Push day").isPublic(isPublic).build();
    }

    private void stored(Workout workout) {
        when(repository.findById(id)).thenReturn(Optional.of(workout));
    }

    private static void assertNotFound(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOf(WorkoutException.class)
                .extracting(e -> ((WorkoutException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Nested
    @DisplayName("getAllWorkouts")
    class GetAllWorkouts {

        @Test
        @DisplayName("queries public workouts plus the caller's own and maps the page")
        void publicPlusOwn() {
            Pageable pageable = PageRequest.of(0, 10);
            Workout w = workout(true);
            WorkoutDTO dto = WorkoutDTO.builder().id(id).build();
            when(repository.findAllByIsPublicTrueOrUser_Username("alice", pageable))
                    .thenReturn(new PageImpl<>(List.of(w), pageable, 1));
            when(mapper.toDTO(w)).thenReturn(dto);

            Connection<WorkoutDTO> result = service.getAllWorkouts(pageable, "alice");

            assertThat(result.results()).containsExactly(dto);
            assertThat(result.totalCount()).isEqualTo(1L);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getWorkoutById / getVisibleWorkout")
    class GetVisibleWorkout {

        @Test
        @DisplayName("public workout is visible to another user")
        void publicToOthers() {
            Workout w = workout(true);
            stored(w);
            WorkoutDTO dto = WorkoutDTO.builder().id(id).build();
            when(mapper.toDTO(w)).thenReturn(dto);

            assertThat(service.getWorkoutById(id, "bob")).isSameAs(dto);
        }

        @Test
        @DisplayName("public workout is visible to anonymous callers")
        void publicToAnonymous() {
            Workout w = workout(true);
            stored(w);

            assertThat(service.getVisibleWorkout(id, null)).isSameAs(w);
        }

        @Test
        @DisplayName("private workout is visible to its owner")
        void privateToOwner() {
            Workout w = workout(false);
            stored(w);

            assertThat(service.getVisibleWorkout(id, "alice")).isSameAs(w);
        }

        @Test
        @DisplayName("private workout reads as not found for another user")
        void privateToOther() {
            stored(workout(false));

            assertNotFound(() -> service.getWorkoutById(id, "bob"));
            verify(mapper, never()).toDTO(any());
        }

        @Test
        @DisplayName("private workout reads as not found for anonymous callers")
        void privateToAnonymous() {
            stored(workout(false));

            assertNotFound(() -> service.getVisibleWorkout(id, null));
        }

        @Test
        @DisplayName("null isPublic is treated as private")
        void nullIsPublic() {
            Workout w = workout(false);
            w.setIsPublic(null);
            stored(w);

            assertNotFound(() -> service.getVisibleWorkout(id, "bob"));
        }

        @Test
        @DisplayName("missing workout is not found")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertNotFound(() -> service.getWorkoutById(id, "alice"));
        }
    }

    @Nested
    @DisplayName("getOwnedWorkout")
    class GetOwnedWorkout {

        @Test
        @DisplayName("returns the workout to its owner")
        void owner() {
            Workout w = workout(false);
            stored(w);

            assertThat(service.getOwnedWorkout(id, "alice")).isSameAs(w);
        }

        @Test
        @DisplayName("someone else's workout reads as not found, even if public")
        void otherUser() {
            stored(workout(true));

            assertNotFound(() -> service.getOwnedWorkout(id, "bob"));
        }

        @Test
        @DisplayName("anonymous caller gets not found")
        void anonymous() {
            stored(workout(true));

            assertNotFound(() -> service.getOwnedWorkout(id, null));
        }

        @Test
        @DisplayName("missing workout is not found")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertNotFound(() -> service.getOwnedWorkout(id, "alice"));
        }
    }

    @Nested
    @DisplayName("getUserWorkoutEntityById")
    class GetUserWorkoutEntityById {

        @Test
        @DisplayName("returns the stored entity")
        void found() {
            Workout w = workout(false);
            stored(w);

            assertThat(service.getUserWorkoutEntityById(id)).isSameAs(w);
        }

        @Test
        @DisplayName("throws not found when missing")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertNotFound(() -> service.getUserWorkoutEntityById(id));
        }
    }

    @Nested
    @DisplayName("deleteWorkoutById")
    class DeleteWorkoutById {

        @Test
        @DisplayName("owner deletes the workout")
        void owner() {
            Workout w = workout(false);
            stored(w);

            MessageResponse response = service.deleteWorkoutById(id, "alice");

            verify(repository).delete(w);
            assertThat(response.message()).isEqualTo("Workout deleted successfully!");
        }

        @Test
        @DisplayName("another user gets not found and nothing is deleted")
        void otherUser() {
            stored(workout(true));

            assertNotFound(() -> service.deleteWorkoutById(id, "bob"));
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("anonymous caller gets not found and nothing is deleted")
        void anonymous() {
            stored(workout(true));

            assertNotFound(() -> service.deleteWorkoutById(id, null));
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updateWorkout")
    class UpdateWorkout {

        @Test
        @DisplayName("null id is rejected before any lookup")
        void idRequired() {
            WorkoutInput input = WorkoutInput.builder().name("x").build();

            assertThatThrownBy(() -> service.updateWorkout(input, "alice"))
                    .isInstanceOf(WorkoutException.class)
                    .extracting(e -> ((WorkoutException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("owner updates: mapped entity is saved")
        void owner() {
            Workout w = workout(false);
            stored(w);
            WorkoutInput input = WorkoutInput.builder().id(id).name("Pull day").build();
            Workout updated = workout(false);
            when(mapper.toUpdateEntity(input, w)).thenReturn(updated);

            MessageResponse response = service.updateWorkout(input, "alice");

            verify(repository).save(updated);
            assertThat(response.message()).isEqualTo("Workout updated successfully!");
        }

        @Test
        @DisplayName("another user gets not found and nothing is saved")
        void otherUser() {
            stored(workout(true));
            WorkoutInput input = WorkoutInput.builder().id(id).name("hijack").build();

            assertNotFound(() -> service.updateWorkout(input, "bob"));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createWorkout")
    class CreateWorkout {

        @Test
        @DisplayName("owner is the caller resolved by username, never from the request")
        void ownerFromUsername() {
            WorkoutInput input = WorkoutInput.builder().name("Leg day").isPublic(true).build();
            Workout mapped = Workout.builder().name("Leg day").user(User.builder().username("mallory").build()).build();
            Workout saved = workout(true);
            WorkoutDTO dto = WorkoutDTO.builder().id(id).build();
            when(mapper.toNewEntity(input)).thenReturn(mapped);
            when(userService.getUserEntityByUsername("alice")).thenReturn(alice);
            when(repository.save(any(Workout.class))).thenReturn(saved);
            when(mapper.toDTO(saved)).thenReturn(dto);

            EntityResponse<WorkoutDTO> response = service.createWorkout(input, "alice");

            ArgumentCaptor<Workout> captor = ArgumentCaptor.forClass(Workout.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isSameAs(alice);
            assertThat(response.result()).isSameAs(dto);
            assertThat(response.message()).isEqualTo("Workout added successfully!");
        }
    }

    @Nested
    @DisplayName("getWorkouts")
    class GetWorkouts {

        @Test
        @DisplayName("lists only the given user's workouts")
        void byUser() {
            Workout w = workout(false);
            WorkoutDTO dto = WorkoutDTO.builder().id(id).build();
            when(repository.findAllByUser_Username("alice")).thenReturn(List.of(w));
            when(mapper.toDTO(w)).thenReturn(dto);

            assertThat(service.getWorkouts("alice")).containsExactly(dto);
        }
    }
}
