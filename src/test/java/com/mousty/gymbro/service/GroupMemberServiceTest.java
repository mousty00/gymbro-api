package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.group_member.GroupMemberInput;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.entity.GroupMember;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.exception.GroupMemberException;
import com.mousty.gymbro.exception.WorkoutGroupException;
import com.mousty.gymbro.mapper.GroupMemberMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.GroupMemberRepository;
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
import org.springframework.http.HttpStatus;

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
class GroupMemberServiceTest {

    @Mock private GroupMemberMapper mapper;
    @Mock private GroupMemberRepository repository;
    @Mock private AuthService authService;
    @Mock private UserService userService;
    @Mock private WorkoutGroupService workoutGroupService;

    private GroupMemberService service;

    private final Pageable pageable = PageRequest.of(0, 10);
    private final UUID groupId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final User alice = User.builder().id(UUID.randomUUID()).username("alice").build(); // group creator
    private final User bob = User.builder().id(UUID.randomUUID()).username("bob").build();     // invited member
    private WorkoutGroup group;
    private GroupMember membership;

    @BeforeEach
    void setUp() {
        service = new GroupMemberService(mapper, repository, authService, userService, workoutGroupService);
        group = WorkoutGroup.builder().id(groupId).createdBy(alice).build();
        membership = GroupMember.builder().id(memberId).group(group).user(bob).status("invited").build();
    }

    private void membershipExists() {
        when(repository.findById(memberId)).thenReturn(Optional.of(membership));
    }

    @Nested
    @DisplayName("getAllGroupMembers")
    class GetAllGroupMembers {

        @Test
        @DisplayName("returns only the caller's own memberships and invitations")
        void callerOnly() {
            GroupMemberDTO dto = GroupMemberDTO.builder().id(memberId).build();
            when(repository.findAllByUser_Username("bob", pageable)).thenReturn(new PageImpl<>(List.of(membership), pageable, 1));
            when(mapper.toDTO(membership)).thenReturn(dto);

            Connection<GroupMemberDTO> result = service.getAllGroupMembers(pageable, "bob");

            assertThat(result.results()).containsExactly(dto);
            assertThat(result.totalCount()).isEqualTo(1L);
            verify(repository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getGroupMemberById")
    class GetGroupMemberById {

        @Test
        @DisplayName("returns the mapped member")
        void found() {
            GroupMemberDTO dto = GroupMemberDTO.builder().id(memberId).build();
            membershipExists();
            when(mapper.toDTO(membership)).thenReturn(dto);

            assertThat(service.getGroupMemberById(memberId)).isSameAs(dto);
        }

        @Test
        @DisplayName("throws not found for unknown id")
        void notFound() {
            when(repository.findById(memberId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getGroupMemberById(memberId))
                    .isInstanceOf(GroupMemberException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getGroupMembersByGroupId")
    class GetGroupMembersByGroupId {

        @Test
        @DisplayName("group member gets the member list")
        void member() {
            GroupMemberDTO dto = GroupMemberDTO.builder().id(memberId).build();
            when(workoutGroupService.getMemberGroup(groupId, "bob")).thenReturn(group);
            when(repository.findAllByGroup_Id(groupId, pageable)).thenReturn(new PageImpl<>(List.of(membership), pageable, 1));
            when(mapper.toDTO(membership)).thenReturn(dto);

            assertThat(service.getGroupMembersByGroupId(groupId, pageable, "bob").results()).containsExactly(dto);
        }

        @Test
        @DisplayName("non-member is rejected and the repository is never queried")
        void nonMember() {
            when(workoutGroupService.getMemberGroup(groupId, "mallory")).thenThrow(WorkoutGroupException.notFound(groupId));

            assertThatThrownBy(() -> service.getGroupMembersByGroupId(groupId, pageable, "mallory"))
                    .isInstanceOf(WorkoutGroupException.class);
            verify(repository, never()).findAllByGroup_Id(any(), any());
        }
    }

    @Nested
    @DisplayName("createGroupMember (invite)")
    class CreateGroupMember {

        private final GroupMemberInput input = GroupMemberInput.builder().groupId(groupId).invitedUsername("bob").build();

        @Test
        @DisplayName("creator invites a user; saved with status invited")
        void creatorInvites() {
            GroupMember newMember = GroupMember.builder().group(group).user(bob).status("invited").build();
            GroupMemberDTO dto = GroupMemberDTO.builder().status("invited").build();
            when(workoutGroupService.getWorkoutGroupEntityById(groupId)).thenReturn(group);
            when(userService.getUserEntityByUsername("bob")).thenReturn(bob);
            when(repository.existsByUserIdAndGroup_Id(bob.getId(), groupId)).thenReturn(false);
            when(mapper.toNewEntity(input)).thenReturn(newMember);
            when(repository.save(any(GroupMember.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDTO(newMember)).thenReturn(dto);

            var response = service.createGroupMember(input, "alice");

            verify(authService).checkAuthorization(eq(alice), eq("alice"), anyString());
            ArgumentCaptor<GroupMember> saved = ArgumentCaptor.forClass(GroupMember.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo("invited");
            assertThat(saved.getValue().getUser()).isSameAs(bob);
            assertThat(response.result()).isSameAs(dto);
        }

        @Test
        @DisplayName("non-creator cannot invite and nothing is saved")
        void nonCreatorForbidden() {
            when(workoutGroupService.getWorkoutGroupEntityById(groupId)).thenReturn(group);
            doThrow(AuthException.forbidden("x")).when(authService).checkAuthorization(eq(alice), eq("bob"), anyString());

            assertThatThrownBy(() -> service.createGroupMember(input, "bob")).isInstanceOf(AuthException.class);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("inviting an existing member fails, checked by the invited user's id")
        void alreadyMember() {
            when(workoutGroupService.getWorkoutGroupEntityById(groupId)).thenReturn(group);
            when(userService.getUserEntityByUsername("bob")).thenReturn(bob);
            when(repository.existsByUserIdAndGroup_Id(bob.getId(), groupId)).thenReturn(true);

            assertThatThrownBy(() -> service.createGroupMember(input, "alice"))
                    .isInstanceOf(GroupMemberException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
            verify(repository, never()).existsByUserIdAndGroup_Id(alice.getId(), groupId);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("unknown group fails and nothing is saved")
        void unknownGroup() {
            when(workoutGroupService.getWorkoutGroupEntityById(groupId)).thenThrow(WorkoutGroupException.notFound(groupId));

            assertThatThrownBy(() -> service.createGroupMember(input, "alice")).isInstanceOf(WorkoutGroupException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteGroupMemberById")
    class DeleteGroupMemberById {

        @Test
        @DisplayName("member can leave")
        void memberLeaves() {
            membershipExists();
            service.deleteGroupMemberById(memberId, "bob");
            verify(repository).delete(membership);
        }

        @Test
        @DisplayName("group creator can remove a member")
        void creatorRemoves() {
            membershipExists();
            service.deleteGroupMemberById(memberId, "alice");
            verify(repository).delete(membership);
        }

        @Test
        @DisplayName("third user is unauthorized and nothing is deleted")
        void thirdUser() {
            membershipExists();

            assertThatThrownBy(() -> service.deleteGroupMemberById(memberId, "mallory"))
                    .isInstanceOf(GroupMemberException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("null username is unauthorized and nothing is deleted")
        void nullUsername() {
            membershipExists();

            assertThatThrownBy(() -> service.deleteGroupMemberById(memberId, null))
                    .isInstanceOf(GroupMemberException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("unknown id throws not found")
        void notFound() {
            when(repository.findById(memberId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteGroupMemberById(memberId, "bob"))
                    .isInstanceOf(GroupMemberException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("acceptGroupMemberInvitation")
    class Accept {

        @Test
        @DisplayName("invited user accepts; status becomes accepted")
        void invitedAccepts() {
            membershipExists();

            service.acceptGroupMemberInvitation(memberId, "bob");

            verify(authService).checkAuthorization(eq(bob), eq("bob"), anyString());
            verify(repository).save(membership);
            assertThat(membership.getStatus()).isEqualTo("accepted");
        }

        @Test
        @DisplayName("an already accepted membership can't be accepted again (not found, nothing saved)")
        void notPending() {
            membership.setStatus("accepted");
            membershipExists();

            assertThatThrownBy(() -> service.acceptGroupMemberInvitation(memberId, "bob"))
                    .isInstanceOf(GroupMemberException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("anyone else is forbidden and nothing is saved")
        void otherForbidden() {
            membershipExists();
            doThrow(AuthException.forbidden("x")).when(authService).checkAuthorization(eq(bob), eq("alice"), anyString());

            assertThatThrownBy(() -> service.acceptGroupMemberInvitation(memberId, "alice")).isInstanceOf(AuthException.class);
            verify(repository, never()).save(any());
            assertThat(membership.getStatus()).isEqualTo("invited");
        }

        @Test
        @DisplayName("unknown id throws not found")
        void notFound() {
            when(repository.findById(memberId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.acceptGroupMemberInvitation(memberId, "bob")).isInstanceOf(GroupMemberException.class);
        }
    }

    @Nested
    @DisplayName("rejectGroupMemberInvitation")
    class Reject {

        @Test
        @DisplayName("invited user rejects; membership is deleted")
        void invitedRejects() {
            membershipExists();

            service.rejectGroupMemberInvitation(memberId, "bob");

            verify(authService).checkAuthorization(eq(bob), eq("bob"), anyString());
            verify(repository).delete(membership);
        }

        @Test
        @DisplayName("an accepted member can't use reject to leave (not found, nothing deleted)")
        void acceptedMemberCannotReject() {
            membership.setStatus("accepted");
            membershipExists();

            assertThatThrownBy(() -> service.rejectGroupMemberInvitation(memberId, "bob"))
                    .isInstanceOf(GroupMemberException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("anyone else is forbidden and nothing is deleted")
        void otherForbidden() {
            membershipExists();
            doThrow(AuthException.forbidden("x")).when(authService).checkAuthorization(eq(bob), eq("alice"), anyString());

            assertThatThrownBy(() -> service.rejectGroupMemberInvitation(memberId, "alice")).isInstanceOf(AuthException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("unknown id throws not found")
        void notFound() {
            when(repository.findById(memberId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rejectGroupMemberInvitation(memberId, "bob")).isInstanceOf(GroupMemberException.class);
            verify(repository, never()).delete(any());
        }
    }
}
