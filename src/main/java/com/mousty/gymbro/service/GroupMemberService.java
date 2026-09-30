package com.mousty.gymbro.service;

import com.mousty.gymbro.entity.GroupMember;
import com.mousty.gymbro.exception.GroupMemberException;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.mapper.GroupMemberMapper;
import com.mousty.gymbro.repository.GroupMemberRepository;
import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.group_member.GroupMemberInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Transactional(readOnly = true)
@Service
public class GroupMemberService extends GenericService<GroupMember, GroupMemberDTO, GroupMemberMapper, GroupMemberRepository> {

    private final AuthService authService;
    private final UserService userService;
    private final WorkoutGroupService workoutGroupService;

    public GroupMemberService(final GroupMemberMapper mapper, final GroupMemberRepository repository,
                              final AuthService authService, final UserService userService,
                              final WorkoutGroupService workoutGroupService) {
        super(mapper, repository);
        this.authService = authService;
        this.userService = userService;
        this.workoutGroupService = workoutGroupService;
    }

    /** The caller's own memberships and pending invitations. */
    public Connection<GroupMemberDTO> getAllGroupMembers(Pageable pageable, String username) {
        return toConnection(repository.findAllByUser_Username(username, pageable));
    }

    public GroupMemberDTO getGroupMemberById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> GroupMemberException.notFound(id));
    }

    /** Member list of a group — only for its creator and accepted members. */
    public Connection<GroupMemberDTO> getGroupMembersByGroupId(UUID groupId, Pageable pageable, String username) {
        workoutGroupService.getMemberGroup(groupId, username);
        return toConnection(repository.findAllByGroup_Id(groupId, pageable));
    }

    private Connection<GroupMemberDTO> toConnection(Page<GroupMember> page) {
        final List<GroupMemberDTO> listDTO = page
                .map(mapper::toDTO)
                .toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());

        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    @Transactional
    public MessageResponse deleteGroupMemberById(UUID id, String username) {
        final GroupMember member = repository.findById(id)
                .orElseThrow(() -> GroupMemberException.notFound(id));
        // the member can leave; the group creator can remove anyone
        if (username == null || (!member.getUser().getUsername().equals(username)
                && !member.getGroup().getCreatedBy().getUsername().equals(username))) {
            throw GroupMemberException.unauthorized();
        }
        repository.delete(member);
        return MessageResponse.builder()
                .message("Group member deleted successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public EntityResponse<GroupMemberDTO> createGroupMember(GroupMemberInput request, String username) {
        // only the group's creator can invite
        final WorkoutGroup group = workoutGroupService.getWorkoutGroupEntityById(request.groupId());
        authService.checkAuthorization(group.getCreatedBy(), username, "User not authorized to add group member");
        final User invited = userService.getUserEntityByUsername(request.invitedUsername());
        if (repository.existsByUserIdAndGroup_Id(invited.getId(), request.groupId())) {
            throw GroupMemberException.alreadyMember();
        }
        final GroupMember newMember = mapper.toNewEntity(request);
        newMember.setStatus("invited");
        final GroupMember invitedMember = repository.save(newMember);
        return EntityResponse.<GroupMemberDTO>builder()
                        .message("User invited successfully")
                        .result(mapper.toDTO(invitedMember))
                        .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public MessageResponse acceptGroupMemberInvitation(UUID id, String username) {
        final GroupMember groupMember = getPendingInvitation(id, username, "User not authorized to accept this invitation");
        groupMember.setStatus("accepted");
        repository.save(groupMember);

        return MessageResponse.builder()
                .message("Invitation accepted successfully")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public MessageResponse rejectGroupMemberInvitation(UUID id, String username) {
        final GroupMember groupMember = getPendingInvitation(id, username, "User not authorized to reject this invitation");
        repository.delete(groupMember);

        return MessageResponse.builder()
                .message("Invitation rejected successfully")
                .timestamp(Instant.now())
                .build();
    }

    // Only the invited user, and only while the invitation is still pending: an accepted member
    // leaves the group via deleteGroupMemberById, not by "rejecting" it.
    private GroupMember getPendingInvitation(UUID id, String username, String forbiddenMessage) {
        final GroupMember groupMember = repository.findById(id)
                .orElseThrow(() -> GroupMemberException.notFound(id));
        authService.checkAuthorization(groupMember.getUser(), username, forbiddenMessage);
        if (!"invited".equals(groupMember.getStatus())) {
            throw GroupMemberException.notFound(id);
        }
        return groupMember;
    }
}
