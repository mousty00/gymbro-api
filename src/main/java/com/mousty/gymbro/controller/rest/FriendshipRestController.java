package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.FriendshipService;
import com.mousty.gymbro.dto.friendship.FriendshipDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/friendship")
@RequiredArgsConstructor
@Tag(name = "Friendship", description = "Friendship management APIs")
public class FriendshipRestController {

    private final FriendshipService service;

    @Operation(summary = "Get all friendships for current user", description = "Returns a paginated list of friendships")
    @GetMapping
    public Connection<FriendshipDTO> getAllFriendships(
            @PageableDefault Pageable pageable,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.getAllFriendships(pageable, username);
    }

    @Operation(summary = "Get friends posts", description = "Retrieve all posts for a specific user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user posts")
    @GetMapping("/posts")
    public List<PostDTO> getAllFriendsPosts(
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ) {
        return service.getAllFriendsPosts(username);
    }

    @Operation(summary = "Search friends by username", description = "Returns a list of friendships matching the username")
    @GetMapping("search")
    public List<FriendshipDTO> findFriendsByUsername(@Valid @NotBlank @RequestParam String username) {
        return service.findFriendsByUsername(username);
    }

    @Operation(summary = "Get friendship by ID", description = "Returns a friendship by its ID")
    @GetMapping("/{id}")
    public FriendshipDTO getFriendshipById(@PathVariable @Valid @NotNull UUID id) {
        return service.getFriendshipById(id);
    }

    @Operation(summary = "Create friendship", description = "Creates a new friendship request")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    public MessageResponse createFriendship(
            @RequestParam @Valid @NotBlank final String friendUsername,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.addFriend(friendUsername, username);
    }

    @Operation(summary = "Accept friendship", description = "Accepts a pending friendship request")
    @GetMapping("/accept/{id}")
    public MessageResponse acceptFriend(@PathVariable @Valid @NotNull UUID id,
                                                        @CurrentSecurityContext(expression = "authentication?.name")
                                                        String username) {
        return service.acceptFriend(id, username);
    }

    @Operation(summary = "Reject friendship", description = "Rejects a pending friendship request")
    @GetMapping("/reject/{id}")
    public MessageResponse rejectFriend(@PathVariable @Valid @NotNull UUID id,
                                                        @CurrentSecurityContext(expression = "authentication?.name")
                                                        String username) {
        return service.rejectFriend(id, username);
    }

    @Operation(summary = "Block friend", description = "Blocks an existing friendship")
    @GetMapping("/block/{id}")
    public MessageResponse blockFriend(
            @PathVariable @Valid @NotNull UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.blockFriend(id, username);
    }


}
