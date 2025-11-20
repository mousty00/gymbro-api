package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.dto.post_comment.CommentDTO;
import com.mousty.gymbro.dto.post_comment.CommentInput;
import com.mousty.gymbro.dto.post_comment.SimpleCommentDTO;
import com.mousty.gymbro.service.CommentService;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comments", description = "Comments management APIs")
@RequiredArgsConstructor
public class CommentRestController {

    private final CommentService commentService;

    @Operation(summary = "Get all comments", description = "Returns a paginated list of all comments")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Connection<CommentDTO>> getAllComments(
            @Parameter(description = "Pagination information")
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(commentService.getAllComments(pageable));
    }

    @Operation(summary = "Get comment by ID", description = "Returns a comment by its ID")
    @GetMapping("{id}")
    public CommentDTO getCommentById(@Parameter(description = "Comment ID") @PathVariable UUID id) {
        return commentService.getCommentById(id);
    }

    @Operation(summary = "Delete comment", description = "Deletes a comment by its ID")
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse deleteCommentById(
            @Parameter(description = "Comment ID")
            @PathVariable UUID id,
            @Parameter(hidden = true)
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ) {
        return commentService.deleteById(id, username);
    }

    @Operation(summary = "Update comment", description = "Updates an existing comment")
    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse updateComment(
            @Parameter(description = "Comment details")
            @Validated @RequestBody
            SimpleCommentDTO request,
            @Parameter(hidden = true)
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ) {
        return commentService.updateComment(request, username);
    }

    @Operation(summary = "Create comment", description = "Creates a new comment")
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public EntityResponse<CommentDTO> createComment(
            @Parameter(description = "Comment to create")
            @Validated @RequestBody CommentInput request,
            @Parameter(hidden = true)
            @CurrentSecurityContext(expression = "authentication?.name")
            String username
    ) {
        return commentService.createComment(request, username);
    }

}
