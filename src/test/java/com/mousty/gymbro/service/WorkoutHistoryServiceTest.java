package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.workout_history.WorkoutHistoryDTO;
import com.mousty.gymbro.dto.workout_history.WorkoutHistoryInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.entity.WorkoutHistory;
import com.mousty.gymbro.exception.WorkoutException;
import com.mousty.gymbro.exception.WorkoutGroupException;
import com.mousty.gymbro.exception.WorkoutHistoryException;
import com.mousty.gymbro.mapper.WorkoutHistoryMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.WorkoutHistoryRepository;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
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
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutHistoryServiceTest {

    @Mock private WorkoutHistoryMapper mapper;
    @Mock private WorkoutHistoryRepository repository;
    @Mock private WorkoutService workoutService;
    @Mock private WorkoutGroupService workoutGroupService;

    private WorkoutHistoryService service;

    private final UUID id = UUID.randomUUID();
    private final UUID workoutId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final WorkoutGroup group = WorkoutGroup.builder().id(groupId).build();
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        service = new WorkoutHistoryService(mapper, repository, workoutService, workoutGroupService);
    }

    private WorkoutHistory history(WorkoutGroup group) {
        WorkoutHistory h = new WorkoutHistory();
        h.setId(id);
        h.setUser(User.builder().username("alice").build());
        h.setGroup(group);
        h.setNotes("old");
        return h;
    }

    private WorkoutHistory stored(WorkoutGroup group) {
        WorkoutHistory h = history(group);
        when(repository.findById(id)).thenReturn(Optional.of(h));
        return h;
    }

    private WorkoutHistoryInput input(UUID groupId) {
        return WorkoutHistoryInput.builder()
                .workoutId(workoutId).groupId(groupId).startedAt(Instant.now()).notes("new").build();
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run)
                .isInstanceOf(WorkoutHistoryException.class)
                .extracting(e -> ((WorkoutHistoryException) e).getStatus())
                .isEqualTo(status);
    }

    @Nested
    @DisplayName("getUserWorkoutHistories")
    class GetUserHistories {

        @Test
        @DisplayName("queries only the caller's histories")
        void onlyCallers() {
            WorkoutHistory h = history(null);
            WorkoutHistoryDTO dto = WorkoutHistoryDTO.builder().id(id).build();
            when(repository.findAllByUser_Username("alice", pageable)).thenReturn(new PageImpl<>(List.of(h), pageable, 1));
            when(mapper.toDTO(h)).thenReturn(dto);

            Connection<WorkoutHistoryDTO> result = service.getUserWorkoutHistories("alice", pageable);

            assertThat(result.results()).containsExactly(dto);
            assertThat(result.totalCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getGroupWorkoutHistories")
    class GetGroupHistories {

        @Test
        @DisplayName("member gets the group's histories")
        void member() {
            WorkoutHistory h = history(group);
            WorkoutHistoryDTO dto = WorkoutHistoryDTO.builder().id(id).build();
            when(workoutGroupService.getMemberGroup(groupId, "alice")).thenReturn(group);
            when(repository.findAllByGroup_Id(groupId, pageable)).thenReturn(new PageImpl<>(List.of(h), pageable, 1));
            when(mapper.toDTO(h)).thenReturn(dto);

            assertThat(service.getGroupWorkoutHistories(groupId, pageable, "alice").results()).containsExactly(dto);
        }

        @Test
        @DisplayName("non-member: group exception propagates and repository is not queried")
        void nonMember() {
            when(workoutGroupService.getMemberGroup(groupId, "bob")).thenThrow(WorkoutGroupException.notFound(groupId));

            assertThatThrownBy(() -> service.getGroupWorkoutHistories(groupId, pageable, "bob"))
                    .isInstanceOf(WorkoutGroupException.class);
            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("getWorkoutHistoryById")
    class GetById {

        @Test
        @DisplayName("owner sees their history")
        void owner() {
            WorkoutHistory h = stored(null);
            WorkoutHistoryDTO dto = WorkoutHistoryDTO.builder().id(id).build();
            when(mapper.toDTO(h)).thenReturn(dto);

            assertThat(service.getWorkoutHistoryById(id, "alice")).isSameAs(dto);
        }

        @Test
        @DisplayName("group member sees a group session")
        void groupMember() {
            WorkoutHistory h = stored(group);
            WorkoutHistoryDTO dto = WorkoutHistoryDTO.builder().id(id).build();
            when(workoutGroupService.isMember(group, "bob")).thenReturn(true);
            when(mapper.toDTO(h)).thenReturn(dto);

            assertThat(service.getWorkoutHistoryById(id, "bob")).isSameAs(dto);
        }

        @Test
        @DisplayName("non-owner non-member of the group: not found")
        void nonMember() {
            stored(group);
            when(workoutGroupService.isMember(group, "bob")).thenReturn(false);

            assertStatus(() -> service.getWorkoutHistoryById(id, "bob"), HttpStatus.NOT_FOUND);
            verify(mapper, never()).toDTO(any());
        }

        @Test
        @DisplayName("non-owner of a solo session: not found without a membership check")
        void noGroup() {
            stored(null);

            assertStatus(() -> service.getWorkoutHistoryById(id, "bob"), HttpStatus.NOT_FOUND);
            verifyNoInteractions(workoutGroupService);
        }

        @Test
        @DisplayName("anonymous caller on a solo session: not found")
        void anonymous() {
            stored(null);

            assertStatus(() -> service.getWorkoutHistoryById(id, null), HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("missing history: not found")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertStatus(() -> service.getWorkoutHistoryById(id, "alice"), HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createWorkoutHistory")
    class Create {

        @Test
        @DisplayName("solo session on a visible workout is saved without a group check")
        void solo() {
            WorkoutHistoryInput request = input(null);
            WorkoutHistory mapped = history(null);
            WorkoutHistoryDTO dto = WorkoutHistoryDTO.builder().id(id).build();
            when(mapper.toNewEntity(request, "alice")).thenReturn(mapped);
            when(mapper.toDTO(mapped)).thenReturn(dto);

            EntityResponse<WorkoutHistoryDTO> response = service.createWorkoutHistory(request, "alice");

            verify(workoutService).getVisibleWorkout(workoutId, "alice");
            verifyNoInteractions(workoutGroupService);
            verify(repository).save(mapped);
            assertThat(response.result()).isSameAs(dto);
        }

        @Test
        @DisplayName("group session requires membership and is saved")
        void group() {
            WorkoutHistoryInput request = input(groupId);
            WorkoutHistory mapped = history(group);
            when(workoutGroupService.getMemberGroup(groupId, "alice")).thenReturn(group);
            when(mapper.toNewEntity(request, "alice")).thenReturn(mapped);

            service.createWorkoutHistory(request, "alice");

            verify(workoutGroupService).getMemberGroup(groupId, "alice");
            verify(repository).save(mapped);
        }

        @Test
        @DisplayName("workout not visible: nothing saved")
        void workoutNotVisible() {
            when(workoutService.getVisibleWorkout(workoutId, "bob")).thenThrow(WorkoutException.notFound(workoutId));

            assertThatThrownBy(() -> service.createWorkoutHistory(input(groupId), "bob"))
                    .isInstanceOf(WorkoutException.class);
            verify(repository, never()).save(any());
            verifyNoInteractions(workoutGroupService);
        }

        @Test
        @DisplayName("not a member of the group: nothing saved")
        void notMember() {
            when(workoutGroupService.getMemberGroup(groupId, "bob")).thenThrow(WorkoutGroupException.notFound(groupId));

            assertThatThrownBy(() -> service.createWorkoutHistory(input(groupId), "bob"))
                    .isInstanceOf(WorkoutGroupException.class);
            verify(repository, never()).save(any());
            verify(mapper, never()).toNewEntity(any(), any());
        }
    }

    @Nested
    @DisplayName("updateWorkoutHistory")
    class Update {

        @Test
        @DisplayName("owner updates the history")
        void owner() {
            WorkoutHistory h = stored(null);
            WorkoutHistoryInput request = input(null);
            when(mapper.toUpdateEntity(request, h)).thenReturn(h);

            MessageResponse response = service.updateWorkoutHistory(id, request, "alice");

            verify(repository).save(h);
            assertThat(response.message()).isEqualTo("Workout history updated successfully!");
        }

        @Test
        @DisplayName("another user is unauthorized and nothing is saved")
        void otherUser() {
            stored(null);

            assertStatus(() -> service.updateWorkoutHistory(id, input(null), "bob"), HttpStatus.FORBIDDEN);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("anonymous caller is unauthorized and nothing is saved")
        void anonymous() {
            stored(null);

            assertStatus(() -> service.updateWorkoutHistory(id, input(null), null), HttpStatus.FORBIDDEN);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("missing history: not found")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertStatus(() -> service.updateWorkoutHistory(id, input(null), "alice"), HttpStatus.NOT_FOUND);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteWorkoutHistory")
    class Delete {

        @Test
        @DisplayName("owner deletes the history")
        void owner() {
            stored(null);

            MessageResponse response = service.deleteWorkoutHistory(id, "alice");

            verify(repository).deleteById(id);
            assertThat(response.message()).isEqualTo("Workout history deleted successfully!");
        }

        @Test
        @DisplayName("group member who is not the owner is unauthorized and nothing is deleted")
        void otherUser() {
            stored(group);

            assertStatus(() -> service.deleteWorkoutHistory(id, "bob"), HttpStatus.FORBIDDEN);
            verify(repository, never()).deleteById(any());
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("anonymous caller is unauthorized and nothing is deleted")
        void anonymous() {
            stored(null);

            assertStatus(() -> service.deleteWorkoutHistory(id, null), HttpStatus.FORBIDDEN);
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("missing history: not found")
        void missing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertStatus(() -> service.deleteWorkoutHistory(id, "alice"), HttpStatus.NOT_FOUND);
            verify(repository, never()).deleteById(any());
        }
    }
}
