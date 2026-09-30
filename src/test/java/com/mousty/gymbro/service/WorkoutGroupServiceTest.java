package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.workout_group.WorkoutGroupDTO;
import com.mousty.gymbro.dto.workout_group.WorkoutGroupInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.entity.Workout;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.exception.WorkoutException;
import com.mousty.gymbro.exception.WorkoutGroupException;
import com.mousty.gymbro.mapper.WorkoutGroupMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.GroupMemberRepository;
import com.mousty.gymbro.repository.WorkoutGroupRepository;
import com.mousty.gymbro.security.auth.AuthService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutGroupServiceTest {

    @Mock private WorkoutGroupMapper mapper;
    @Mock private WorkoutGroupRepository repository;
    @Mock private AuthService authService;
    @Mock private WorkoutService workoutService;
    @Mock private UserService userService;
    @Mock private GroupMemberRepository groupMemberRepository;

    private WorkoutGroupService service;

    private final UUID groupId = UUID.randomUUID();
    private final User alice = User.builder().id(UUID.randomUUID()).username("alice").build();
    private WorkoutGroup group;

    @BeforeEach
    void setUp() {
        service = new WorkoutGroupService(mapper, repository, authService, workoutService, userService, groupMemberRepository);
        group = WorkoutGroup.builder().id(groupId).name("Leg day").createdBy(alice).build();
    }

    private void groupExists() {
        when(repository.findById(groupId)).thenReturn(Optional.of(group));
    }

    private void acceptedMember(String username, boolean accepted) {
        when(groupMemberRepository.existsByUser_UsernameAndGroup_IdAndStatus(username, groupId, "accepted"))
                .thenReturn(accepted);
    }

    @Nested
    @DisplayName("createWorkoutGroup")
    class CreateWorkoutGroup {

        private final UUID workoutId = UUID.randomUUID();
        private final WorkoutGroupInput input = WorkoutGroupInput.builder()
                .name("Leg day").workoutId(workoutId).status("scheduled").build();

        @Test
        @DisplayName("sets createdBy from the caller and saves the group with the visible workout")
        void createsWithCallerAsCreator() {
            Workout workout = Workout.builder().id(workoutId).build();
            WorkoutGroup mapped = WorkoutGroup.builder().name("Leg day").workout(workout).build();
            WorkoutGroupDTO dto = WorkoutGroupDTO.builder().id(groupId).build();
            when(workoutService.getVisibleWorkout(workoutId, "alice")).thenReturn(workout);
            when(mapper.toNewEntity(input, workout, null)).thenReturn(mapped);
            when(userService.getUserEntityByUsername("alice")).thenReturn(alice);
            when(repository.save(any(WorkoutGroup.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDTO(mapped)).thenReturn(dto);

            var response = service.createWorkoutGroup(input, "alice");

            ArgumentCaptor<WorkoutGroup> saved = ArgumentCaptor.forClass(WorkoutGroup.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getCreatedBy()).isSameAs(alice);
            assertThat(saved.getValue().getWorkout()).isSameAs(workout);
            assertThat(response.result()).isSameAs(dto);
        }

        @Test
        @DisplayName("fails and saves nothing when the workout is not visible to the caller")
        void workoutNotVisible() {
            when(workoutService.getVisibleWorkout(workoutId, "bob")).thenThrow(WorkoutException.notFound(workoutId));

            assertThatThrownBy(() -> service.createWorkoutGroup(input, "bob")).isInstanceOf(WorkoutException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllWorkoutGroups")
    class GetAllWorkoutGroups {

        @Test
        @DisplayName("returns only groups visible to the caller")
        void usesVisibilityQuery() {
            Pageable pageable = PageRequest.of(0, 10);
            WorkoutGroupDTO dto = WorkoutGroupDTO.builder().id(groupId).build();
            when(repository.findAllVisibleTo("alice", pageable)).thenReturn(new PageImpl<>(List.of(group), pageable, 1));
            when(mapper.toDTO(group)).thenReturn(dto);

            Connection<WorkoutGroupDTO> result = service.getAllWorkoutGroups(pageable, "alice");

            assertThat(result.results()).containsExactly(dto);
            assertThat(result.totalCount()).isEqualTo(1L);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getWorkoutGroupById / getMemberGroup")
    class GetMemberGroup {

        @Test
        @DisplayName("creator gets the group")
        void creator() {
            groupExists();
            WorkoutGroupDTO dto = WorkoutGroupDTO.builder().id(groupId).build();
            when(mapper.toDTO(group)).thenReturn(dto);

            assertThat(service.getWorkoutGroupById(groupId, "alice")).isSameAs(dto);
        }

        @Test
        @DisplayName("accepted member gets the group")
        void acceptedMemberAllowed() {
            groupExists();
            acceptedMember("bob", true);

            assertThat(service.getMemberGroup(groupId, "bob")).isSameAs(group);
        }

        @Test
        @DisplayName("invited-but-not-accepted user gets not found")
        void invitedOnly() {
            groupExists();
            acceptedMember("bob", false);

            assertThatThrownBy(() -> service.getMemberGroup(groupId, "bob")).isInstanceOf(WorkoutGroupException.class);
        }

        @Test
        @DisplayName("outsider gets not found via getWorkoutGroupById")
        void outsider() {
            groupExists();
            acceptedMember("mallory", false);

            assertThatThrownBy(() -> service.getWorkoutGroupById(groupId, "mallory"))
                    .isInstanceOf(WorkoutGroupException.class)
                    .hasMessageContaining(groupId.toString());
            verify(mapper, never()).toDTO(any());
        }

        @Test
        @DisplayName("null username gets not found")
        void nullUsername() {
            groupExists();

            assertThatThrownBy(() -> service.getMemberGroup(groupId, null)).isInstanceOf(WorkoutGroupException.class);
        }

        @Test
        @DisplayName("unknown id gets not found")
        void unknownId() {
            when(repository.findById(groupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMemberGroup(groupId, "alice")).isInstanceOf(WorkoutGroupException.class);
        }
    }

    @Nested
    @DisplayName("isMember")
    class IsMember {

        @Test
        @DisplayName("creator is a member without querying memberships")
        void creator() {
            assertThat(service.isMember(group, "alice")).isTrue();
            verifyNoInteractions(groupMemberRepository);
        }

        @Test
        @DisplayName("accepted member is a member")
        void accepted() {
            acceptedMember("bob", true);
            assertThat(service.isMember(group, "bob")).isTrue();
        }

        @Test
        @DisplayName("anyone else is not a member")
        void other() {
            acceptedMember("mallory", false);
            assertThat(service.isMember(group, "mallory")).isFalse();
        }

        @Test
        @DisplayName("null username is not a member")
        void nullUsername() {
            assertThat(service.isMember(group, null)).isFalse();
            verifyNoInteractions(groupMemberRepository);
        }
    }

    @Nested
    @DisplayName("getWorkoutGroupEntityById")
    class GetEntityById {

        @Test
        @DisplayName("returns the stored group")
        void found() {
            groupExists();
            assertThat(service.getWorkoutGroupEntityById(groupId)).isSameAs(group);
        }

        @Test
        @DisplayName("throws not found for unknown id")
        void notFound() {
            when(repository.findById(groupId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getWorkoutGroupEntityById(groupId)).isInstanceOf(WorkoutGroupException.class);
        }
    }

    @Nested
    @DisplayName("deleteWorkoutGroup")
    class DeleteWorkoutGroup {

        @Test
        @DisplayName("creator deletes the group, checked against the stored owner")
        void creatorDeletes() {
            groupExists();

            service.deleteWorkoutGroup(groupId, "alice");

            verify(authService).checkAuthorization(eq(alice), eq("alice"), anyString());
            verify(repository).delete(group);
        }

        @Test
        @DisplayName("non-creator is forbidden and nothing is deleted")
        void forbidden() {
            groupExists();
            doThrow(AuthException.forbidden("x")).when(authService).checkAuthorization(eq(alice), eq("bob"), anyString());

            assertThatThrownBy(() -> service.deleteWorkoutGroup(groupId, "bob")).isInstanceOf(AuthException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("unknown id throws not found and nothing is deleted")
        void notFound() {
            when(repository.findById(groupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteWorkoutGroup(groupId, "alice")).isInstanceOf(WorkoutGroupException.class);
            verify(repository, never()).delete(any());
        }
    }
}
