package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.service.LikeService;
import com.mousty.gymbro.dto.post_like.LikeDTO;
import com.mousty.gymbro.dto.post_like.LikeInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.MessageResponse;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.CurrentSecurityContext;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class LikeController {

    private final LikeService service;

    @DgsQuery
    public Connection<LikeDTO> likes(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size){
        return service.getAllLikes(PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 15
        ));
    }

    @DgsQuery
    public LikeDTO like(@InputArgument UUID id){
        return service.getLikeById(id);
    }

    @DgsMutation
    public MessageResponse deleteLike(
            @InputArgument UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username){
        return service.deleteLikeById(id, username);
    }

    @DgsMutation
    public LikeDTO createLike(
            @Valid @InputArgument LikeInput request,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.createLike(request, username);
    }


}
