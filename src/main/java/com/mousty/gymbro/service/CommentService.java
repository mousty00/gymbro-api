package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.post_comment.CommentDTO;
import com.mousty.gymbro.dto.post_comment.CommentInput;
import com.mousty.gymbro.dto.post_comment.SimpleCommentDTO;
import com.mousty.gymbro.exception.CommentException;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.mapper.CommentMapper;
import com.mousty.gymbro.repository.CommentRepository;
import com.mousty.gymbro.entity.PostComment;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Transactional(readOnly = true)
@Service
public class CommentService extends GenericService<PostComment, CommentDTO, CommentMapper, CommentRepository> {

    private final UserService userService;
    private final PostService postService;
    private final AuthService authService;

    public CommentService(final CommentMapper mapper, final CommentRepository repository, final UserService userService, final PostService postService, final AuthService authService) {
        super(mapper, repository);
        this.userService = userService;
        this.postService = postService;
        this.authService = authService;
    }

    public Connection<CommentDTO> getAllComments(Pageable pageable) {
        return getAll(pageable);
    }

    public CommentDTO getCommentById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> CommentException.notFound(id));
    }

    @Transactional
    public MessageResponse deleteById(UUID id, String username) {
        final PostComment comment = getCommentEntityById(id);
        authService.checkAuthorization(comment.getUser(), username, "User not authorized to delete comment");
        repository.delete(comment);
        return MessageResponse.builder()
                .message("Comment deleted successfully!")
                .timestamp(Instant.now())
                .build();
    }

    // Only the content is editable; author and post stay as stored.
    @Transactional
    public MessageResponse updateComment(SimpleCommentDTO request, String username) {
        final PostComment comment = getCommentEntityById(request.id());
        authService.checkAuthorization(comment.getUser(), username, "User not authorized to update comment");
        comment.setContent(request.content());
        repository.save(comment);
        return MessageResponse.builder()
                .message("Comment updated successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public EntityResponse<CommentDTO> createComment(CommentInput request, String username) {
        final User user = userService.getUserEntityByUsername(username);
        final Post post = postService.getPostEntityById(request.postId());
        final PostComment comment = repository.save(mapper.toNewEntity(request, user, post));
        final CommentDTO dto = mapper.toDTO(comment);
        return EntityResponse.<CommentDTO>builder()
                .message("Comment added successfully!")
                .result(dto)
                .timestamp(Instant.now())
                .build();
    }

    public PostComment getCommentEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> CommentException.notFound(id));
    }
}
