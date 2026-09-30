package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.exercise.ExerciseDTO;
import com.mousty.gymbro.dto.exercise.SimpleExerciseDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseDTO;
import com.mousty.gymbro.dto.workout_exercise.WorkoutExerciseInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.entity.WorkoutExercise;
import com.mousty.gymbro.exception.ExerciseException;
import com.mousty.gymbro.exception.WorkoutException;
import com.mousty.gymbro.exception.WorkoutExerciseException;
import com.mousty.gymbro.mapper.ExerciseMapper;
import com.mousty.gymbro.mapper.WorkoutExerciseMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.WorkoutExerciseRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutExerciseServiceTest {

    @Mock private WorkoutExerciseMapper mapper;
    @Mock private WorkoutExerciseRepository repository;
    @Mock private WorkoutService workoutService;
    @Mock private ExerciseService exerciseService;
    @Mock private ExerciseMapper exerciseMapper;

    private WorkoutExerciseService service;

    private final UUID rowId = UUID.randomUUID();
    private final UUID workoutId = UUID.randomUUID();
    private final UUID exerciseId = UUID.randomUUID();
    private final Workout workout = Workout.builder()
            .id(workoutId).user(User.builder().username("alice").build()).isPublic(false).build();
    private final WorkoutExercise row = WorkoutExercise.builder().id(rowId).workout(workout).sets(3).position(1).build();
    private final ExerciseDTO exerciseDto = ExerciseDTO.builder().id(exerciseId).name("Squat").build();
    private final SimpleExerciseDTO simpleExercise = SimpleExerciseDTO.builder().id(exerciseId).name("Squat").build();

    @BeforeEach
    void setUp() {
        service = new WorkoutExerciseService(mapper, repository, workoutService, exerciseService, exerciseMapper);
    }

    private WorkoutExerciseInput input(UUID id) {
        return WorkoutExerciseInput.builder()
                .id(id).workoutId(workoutId).exerciseId(exerciseId)
                .sets(3).reps(10).restSeconds(60).position(1).build();
    }

    private void storedRow() {
        when(repository.findById(rowId)).thenReturn(Optional.of(row));
    }

    private void targetResolvable(String username) {
        when(workoutService.getOwnedWorkout(workoutId, username)).thenReturn(workout);
        when(exerciseService.getExerciseById(exerciseId, username)).thenReturn(exerciseDto);
        when(exerciseMapper.toSimpleDTO(exerciseDto)).thenReturn(simpleExercise);
    }

    @Nested
    @DisplayName("getAllWorkoutExercises")
    class GetAll {

        @Test
        @DisplayName("queries only rows of the caller's own workouts")
        void onlyCallers() {
            Pageable pageable = PageRequest.of(0, 5);
            WorkoutExerciseDTO dto = WorkoutExerciseDTO.builder().id(rowId).build();
            when(repository.findAllByWorkout_User_Username("alice", pageable))
                    .thenReturn(new PageImpl<>(List.of(row), pageable, 1));
            when(mapper.toDTO(row)).thenReturn(dto);

            Connection<WorkoutExerciseDTO> result = service.getAllWorkoutExercises(pageable, "alice");

            assertThat(result.results()).containsExactly(dto);
            assertThat(result.totalCount()).isEqualTo(1L);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getWorkoutExerciseById")
    class GetById {

        @Test
        @DisplayName("returns the row when its parent workout is visible")
        void visible() {
            storedRow();
            WorkoutExerciseDTO dto = WorkoutExerciseDTO.builder().id(rowId).build();
            when(workoutService.getVisibleWorkout(workoutId, null)).thenReturn(workout);
            when(mapper.toDTO(row)).thenReturn(dto);

            assertThat(service.getWorkoutExerciseById(rowId, null)).isSameAs(dto);
        }

        @Test
        @DisplayName("propagates not found when the parent workout is not visible")
        void notVisible() {
            storedRow();
            when(workoutService.getVisibleWorkout(workoutId, "bob")).thenThrow(WorkoutException.notFound(workoutId));

            assertThatThrownBy(() -> service.getWorkoutExerciseById(rowId, "bob"))
                    .isInstanceOf(WorkoutException.class);
            verify(mapper, never()).toDTO(any());
        }

        @Test
        @DisplayName("missing row is not found")
        void missing() {
            when(repository.findById(rowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getWorkoutExerciseById(rowId, "alice"))
                    .isInstanceOf(WorkoutExerciseException.class);
        }
    }

    @Nested
    @DisplayName("createWorkoutExercise")
    class Create {

        @Test
        @DisplayName("forces client-supplied id to null before mapping and saves into the owned workout")
        void idForcedNull() {
            targetResolvable("alice");
            WorkoutExercise mapped = WorkoutExercise.builder().workout(workout).build();
            WorkoutExerciseDTO dto = WorkoutExerciseDTO.builder().id(rowId).build();
            when(mapper.toNewEntity(any(WorkoutExerciseInput.class), eq(workout), eq(simpleExercise))).thenReturn(mapped);
            when(repository.save(mapped)).thenReturn(mapped);
            when(mapper.toDTO(mapped)).thenReturn(dto);

            EntityResponse<WorkoutExerciseDTO> response = service.createWorkoutExercise(input(rowId), "alice");

            ArgumentCaptor<WorkoutExerciseInput> captor = ArgumentCaptor.forClass(WorkoutExerciseInput.class);
            verify(mapper).toNewEntity(captor.capture(), eq(workout), eq(simpleExercise));
            assertThat(captor.getValue().id()).isNull();
            assertThat(captor.getValue().workoutId()).isEqualTo(workoutId);
            assertThat(response.result()).isSameAs(dto);
        }

        @Test
        @DisplayName("target workout not owned: not found and nothing saved")
        void workoutNotOwned() {
            when(workoutService.getOwnedWorkout(workoutId, "bob")).thenThrow(WorkoutException.notFound(workoutId));

            assertThatThrownBy(() -> service.createWorkoutExercise(input(null), "bob"))
                    .isInstanceOf(WorkoutException.class);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("exercise not visible to caller: not found and nothing saved")
        void exerciseNotVisible() {
            when(workoutService.getOwnedWorkout(workoutId, "alice")).thenReturn(workout);
            when(exerciseService.getExerciseById(exerciseId, "alice")).thenThrow(ExerciseException.notFound(exerciseId));

            assertThatThrownBy(() -> service.createWorkoutExercise(input(null), "alice"))
                    .isInstanceOf(ExerciseException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateWorkoutExercise")
    class Update {

        @Test
        @DisplayName("null id is rejected with idRequired")
        void idRequired() {
            assertThatThrownBy(() -> service.updateWorkoutExercise(input(null), "alice"))
                    .isInstanceOf(WorkoutExerciseException.class)
                    .extracting(e -> ((WorkoutExerciseException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("owner updates the row")
        void owner() {
            storedRow();
            targetResolvable("alice");
            WorkoutExerciseInput request = input(rowId);
            WorkoutExercise mapped = WorkoutExercise.builder().id(rowId).workout(workout).build();
            when(mapper.toNewEntity(request, workout, simpleExercise)).thenReturn(mapped);

            MessageResponse response = service.updateWorkoutExercise(request, "alice");

            verify(repository).save(mapped);
            assertThat(response.message()).isEqualTo("Workout exercise updated successfully!");
        }

        @Test
        @DisplayName("existing row's workout not owned: nothing saved")
        void existingNotOwned() {
            storedRow();
            when(workoutService.getOwnedWorkout(workoutId, "bob")).thenThrow(WorkoutException.notFound(workoutId));

            assertThatThrownBy(() -> service.updateWorkoutExercise(input(rowId), "bob"))
                    .isInstanceOf(WorkoutException.class);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("moving the row into a workout the caller does not own: nothing saved")
        void targetNotOwned() {
            storedRow();
            UUID foreignWorkout = UUID.randomUUID();
            WorkoutExerciseInput request = input(rowId).toBuilder().workoutId(foreignWorkout).build();
            when(workoutService.getOwnedWorkout(workoutId, "alice")).thenReturn(workout);
            when(workoutService.getOwnedWorkout(foreignWorkout, "alice")).thenThrow(WorkoutException.notFound(foreignWorkout));

            assertThatThrownBy(() -> service.updateWorkoutExercise(request, "alice"))
                    .isInstanceOf(WorkoutException.class);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("missing row is not found")
        void missing() {
            when(repository.findById(rowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateWorkoutExercise(input(rowId), "alice"))
                    .isInstanceOf(WorkoutExerciseException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteWorkoutExerciseById")
    class Delete {

        @Test
        @DisplayName("owner of the parent workout deletes the row")
        void owner() {
            storedRow();
            when(workoutService.getOwnedWorkout(workoutId, "alice")).thenReturn(workout);

            MessageResponse response = service.deleteWorkoutExerciseById(rowId, "alice");

            verify(repository).delete(row);
            assertThat(response.message()).isEqualTo("Workout exercise deleted successfully!");
        }

        @Test
        @DisplayName("non-owner: not found and nothing deleted")
        void nonOwner() {
            storedRow();
            when(workoutService.getOwnedWorkout(workoutId, null)).thenThrow(WorkoutException.notFound(workoutId));

            assertThatThrownBy(() -> service.deleteWorkoutExerciseById(rowId, null))
                    .isInstanceOf(WorkoutException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("missing row: not found and nothing deleted")
        void missing() {
            when(repository.findById(rowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteWorkoutExerciseById(rowId, "alice"))
                    .isInstanceOf(WorkoutExerciseException.class);
            verify(repository, never()).delete(any());
        }
    }
}
