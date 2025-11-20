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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import java.util.List;
import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService service;

    @DgsQuery
    public Connection<FriendshipDTO> friends(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return  service.getAllFriendships(PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 15
        ), username);
    }

    @DgsQuery
    public List<PostDTO> friendsPosts(
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return service.getAllFriendsPosts(username);
    }

    @DgsQuery
    public List<FriendshipDTO> findFriends(@InputArgument String username){
        return service.findFriendsByUsername(username);
    }

    @DgsQuery
    public FriendshipDTO friend(@InputArgument UUID id){
        return service.getFriendshipById(id);
    }

    @DgsMutation
    public MessageResponse addFriend(
            @Valid @NotBlank final String friendUsername,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return service.addFriend(friendUsername, username);
    }

    @DgsMutation
    public MessageResponse acceptFriend(
            @Valid @NotNull UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return service.acceptFriend(id, username);
    }

    @DgsMutation
    public MessageResponse rejectFriend(
            @Valid @NotNull UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return service.rejectFriend(id, username);
    }

    @DgsMutation
    public MessageResponse blockFriend(
            @Valid @NotNull UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ){
        return service.blockFriend(id, username);
    }

}
