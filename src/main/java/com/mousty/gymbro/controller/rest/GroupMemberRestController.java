package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.GroupMemberService;
import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.group_member.GroupMemberInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/group_members")
@RequiredArgsConstructor
@Tag(name = "Group Members", description = "Group member management APIs")
public class GroupMemberRestController {

    private final GroupMemberService service;

    @Operation(summary = "Get all group members", description = "Returns a paginated list of group members, optionally filtered by group ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved group members")
    @GetMapping
    public Connection<GroupMemberDTO> getAllGroupMembers(@RequestParam UUID groupId, @PageableDefault Pageable pageable) {
        if (groupId != null) {
            return service.getGroupMembersByGroupId(groupId, pageable);
        }
        return service.getAllGroupMembers(pageable);
    }

    @Operation(summary = "Delete group member", description = "Removes a member from a group")
    @ApiResponse(responseCode = "200", description = "Member successfully removed")
    @ApiResponse(responseCode = "404", description = "Member not found")
    @DeleteMapping("/delete/{id}")
    public MessageResponse deleteGroupMemberById(@PathVariable @NotNull UUID id,
                                                                 @CurrentSecurityContext(expression = "authentication?.name")
                                                                 String username) {
        return service.deleteGroupMemberById(id, username);
    }

    @Operation(summary = "Create group member", description = "Adds a new member to a group")
    @ApiResponse(responseCode = "201", description = "Member successfully added")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping("/create")
    public EntityResponse<GroupMemberDTO> createGroupMember(@Valid @RequestBody GroupMemberInput request,
                                                                            @CurrentSecurityContext(expression = "authentication?.name")
                                                                            String username) {
        return service.createGroupMember(request, username);
    }

    @Operation(summary = "Accept group invitation", description = "Accepts a pending group membership invitation")
    @ApiResponse(responseCode = "200", description = "Invitation accepted successfully")
    @ApiResponse(responseCode = "404", description = "Invitation not found")
    @PostMapping("/accept-invitation/{id}")
    public MessageResponse acceptGroupMemberInvitation(@PathVariable @NotNull UUID id,
                                                                            @CurrentSecurityContext(expression = "authentication?.name")
                                                                            String username) {
        return service.acceptGroupMemberInvitation(id, username);
    }

    @Operation(summary = "Reject group invitation", description = "Rejects a pending group membership invitation")
    @ApiResponse(responseCode = "200", description = "Invitation rejected successfully")
    @ApiResponse(responseCode = "404", description = "Invitation not found")
    @PostMapping("/reject-invitation/{id}")
    public MessageResponse rejectGroupMemberInvitation(@PathVariable @NotNull UUID id,
                                                                       @CurrentSecurityContext(expression = "authentication?.name")
                                                                       String username) {
        return service.rejectGroupMemberInvitation(id, username);
    }


}
