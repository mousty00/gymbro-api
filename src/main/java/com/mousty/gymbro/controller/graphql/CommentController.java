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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.mousty.gymbro.generic.PageableDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mousty.gymbro.security.CurrentUsername;

import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CommentController {

    private final CommentService service;

    @DgsQuery
    public Connection<CommentDTO> comments(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size
    ) {
        return service.getAllComments(PageableDefaults.INSTANCE.create(page, size));
    }

    @DgsQuery
    public CommentDTO comment(@InputArgument UUID id) {
        return service.getCommentById(id);
    }

    @DgsMutation
    public MessageResponse deleteComment(
            @InputArgument UUID id,
            @CurrentUsername
            String username) {
        return service.deleteById(id, username);
    }

    @DgsMutation
    public EntityResponse<CommentDTO> createComment(
            @Valid @InputArgument CommentInput request,
            @CurrentUsername
            String username) {
        return service.createComment(request, username);
    }

}
