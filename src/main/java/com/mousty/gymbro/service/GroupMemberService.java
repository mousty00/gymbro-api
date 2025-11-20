package com.mousty.gymbro.service;

import com.mousty.gymbro.entity.GroupMember;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.mapper.GroupMemberMapper;
import com.mousty.gymbro.repository.GroupMemberRepository;
import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.group_member.GroupMemberInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GroupMemberService extends GenericService<GroupMember, GroupMemberDTO, GroupMemberMapper, GroupMemberRepository> {

    private final GroupMemberMapper mapper;
    private final GroupMemberRepository repository;
    private final AuthService authService;
    private final UserService userService;

    public GroupMemberService(final GroupMemberMapper mapper, final GroupMemberRepository repository,
                              final AuthService authService, final UserService userService) {
        super(mapper, repository);
        this.repository = repository;
        this.mapper = mapper;
        this.authService = authService;
        this.userService = userService;
    }

    public Connection<GroupMemberDTO> getAllGroupMembers(@PageableDefault Pageable pageable) {
        return getAll(pageable);
    }

    public GroupMemberDTO getGroupMemberById(UUID id) {
        return getById(id, "Group member not found");
    }

    public Connection<GroupMemberDTO> getGroupMembersByGroupId(UUID groupId, Pageable pageable) {
        final Page<GroupMember> page = repository.findAllByGroup_Id(groupId, pageable);
        final List<GroupMemberDTO> listDTO = page
                .map(mapper::toDTO)
                .toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());

        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    @Transactional
    public MessageResponse deleteGroupMemberById(UUID id, String username) {
        authService.checkAuthorization(id, username, "User not authorized to delete group member");
        return delete(id, "Group member not found", "Group member deleted successfully!");
    }

    public EntityResponse<GroupMemberDTO> createGroupMember(GroupMemberInput request,
                                                                                        String username) {
        final User loggedUser = userService.getUserEntityByUsername(username);
        authService.checkAuthorization(loggedUser.getId(), username, "User not authorized to add group member");
        if (repository.existsByUserIdAndGroup_Id(loggedUser.getId(), request.getGroupId())) {
            throw new IllegalStateException("User is already a member of this group");
        }
        final GroupMember invitedMember = repository.save(mapper.toNewEntity(request));
        invitedMember.setStatus("invited");
        return EntityResponse.<GroupMemberDTO>builder()
                        .message("user invited successfully")
                        .result(mapper.toDTO(invitedMember))
                        .timestamp(Instant.now())
                .build();
    }
    
    public MessageResponse acceptGroupMemberInvitation(UUID id, String username) {
        final GroupMember groupMember = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group member not found"));

        authService.checkAuthorization(groupMember.getUser().getId(), username,
                "User not authorized to accept this invitation");

        groupMember.setStatus("accepted");
        repository.save(groupMember);

        return MessageResponse.builder()
                .message("Invitation accepted successfully")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public MessageResponse rejectGroupMemberInvitation(UUID id, String username) {
        final UUID UserIdByGroupMemberId = getGroupMemberById(id).getUser().getId();

        authService.checkAuthorization(UserIdByGroupMemberId, username,
                "User not authorized to accept this invitation");

        repository.deleteGroupMemberById(id);

        return MessageResponse.builder()
                .message("Invitation rejected successfully")
                .timestamp(Instant.now())
                .build();
    }


}
