package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.service.FriendshipService;
import com.mousty.gymbro.dto.friendship.FriendshipDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.MessageResponse;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import com.mousty.gymbro.generic.PageableDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mousty.gymbro.security.CurrentUsername;
import java.util.List;
import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendshipController {

    private final FriendshipService service;

    @DgsQuery
    public Connection<FriendshipDTO> friends(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size,
            @CurrentUsername
            String username
    ){
        return  service.getAllFriendships(PageableDefaults.INSTANCE.create(page, size), username);
    }

    @DgsQuery
    public List<PostDTO> friendsPosts(
            @CurrentUsername
            String username
    ){
        return service.getAllFriendsPosts(username);
    }

    @DgsQuery
    public List<FriendshipDTO> findFriends(@InputArgument String username){
        return service.findFriendsByUsername(username);
    }

    @DgsQuery
    public FriendshipDTO friend(@InputArgument UUID id, @CurrentUsername String username){
        return service.getFriendshipById(id, username);
    }

    @DgsQuery
    public List<FriendshipDTO> friendRequests(@CurrentUsername String username){
        return service.getPendingRequests(username);
    }

    @DgsMutation
    public MessageResponse addFriend(
            @Valid @NotBlank final String friendUsername,
            @CurrentUsername
            String username
    ){
        return service.addFriend(friendUsername, username);
    }

    @DgsMutation
    public MessageResponse acceptFriend(
            @Valid @NotNull UUID id,
            @CurrentUsername
            String username
    ){
        return service.acceptFriend(id, username);
    }

    @DgsMutation
    public MessageResponse rejectFriend(
            @Valid @NotNull UUID id,
            @CurrentUsername
            String username
    ){
        return service.rejectFriend(id, username);
    }

    @DgsMutation
    public MessageResponse blockFriend(
            @Valid @NotNull UUID id,
            @CurrentUsername
            String username
    ){
        return service.blockFriend(id, username);
    }

}
