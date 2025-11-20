package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.dto.post_comment.CommentDTO;
import com.mousty.gymbro.dto.post_comment.CommentInput;
import com.mousty.gymbro.service.CommentService;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.CurrentSecurityContext;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class CommentController {

    private final CommentService service;

    @DgsQuery
    public Connection<CommentDTO> comments(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size
    ) {
        return service.getAllComments(PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 15));
    }

    @DgsQuery
    public CommentDTO comment(@InputArgument UUID id) {
        return service.getCommentById(id);
    }

    @DgsMutation
    public MessageResponse deleteComment(
            @InputArgument UUID id,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.deleteById(id, username);
    }

    @DgsMutation
    public EntityResponse<CommentDTO> createComment(
            @InputArgument CommentInput request,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.createComment(request, username);
    }

}
