package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.group_member.GroupMemberInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.service.GroupMemberService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import com.mousty.gymbro.generic.PageableDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mousty.gymbro.security.CurrentUsername;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GroupMemberController {
    private final GroupMemberService service;

    @DgsQuery
    public Connection<GroupMemberDTO> groupMembers(
            @InputArgument @Nullable UUID id,
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size,
            @CurrentUsername String username) {
        Pageable pageable = PageableDefaults.INSTANCE.create(page, size);
        if (id != null) {
            return service.getGroupMembersByGroupId(id, pageable, username);
        }
        return service.getAllGroupMembers(pageable, username);
    }

    @DgsMutation
    public MessageResponse removeGroupMember(
            @Valid @NotNull @InputArgument UUID id,
            @CurrentUsername String username) {
        return service.deleteGroupMemberById(id, username);
    }

    @DgsMutation
    public EntityResponse<GroupMemberDTO> addGroupMember(
            @Valid @InputArgument GroupMemberInput request,
            @CurrentUsername String username) {
        return service.createGroupMember(request, username);
    }

    @DgsMutation
    public MessageResponse acceptGroupInvitation(
            @Valid @InputArgument UUID id,
            @CurrentUsername String username) {
        return service.acceptGroupMemberInvitation(id, username);
    }

    @DgsMutation
    public MessageResponse rejectGroupInvitation(
            @Valid @InputArgument UUID id,
            @CurrentUsername String username) {
        return service.rejectGroupMemberInvitation(id, username);
    }
}
