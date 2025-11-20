package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.service.GroupMemberService;
import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.group_member.GroupMemberInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.CurrentSecurityContext;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class GroupMemberController {
    private final GroupMemberService service;

    @DgsQuery
    public Connection<GroupMemberDTO> groupMembers(
            @InputArgument @Nullable UUID id,
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size)
    {
        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 15
        );
        if (id != null) {
            return service.getGroupMembersByGroupId(id, pageable);
        }
        return service.getAllGroupMembers(pageable);
    }

    @DgsMutation
    public MessageResponse removeGroupMember(
            @Valid @NotNull @InputArgument UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return service.deleteGroupMemberById(id, username);
    }

    @DgsMutation
    public EntityResponse<GroupMemberDTO> addGroupMember(
            @Valid @InputArgument GroupMemberInput request,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return service.createGroupMember(request, username);
    }

    @DgsMutation
    public MessageResponse acceptGroupInvitation(
            @Valid @InputArgument UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return service.acceptGroupMemberInvitation(id, username);
    }
}
