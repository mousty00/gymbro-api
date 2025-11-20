package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.post_comment.CommentDTO;
import com.mousty.gymbro.dto.post_comment.CommentInput;
import com.mousty.gymbro.dto.post_comment.SimpleCommentDTO;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.mapper.CommentMapper;
import com.mousty.gymbro.repository.CommentRepository;
import com.mousty.gymbro.entity.PostComment;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.repository.UserRepository;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class CommentService extends GenericService<PostComment, CommentDTO, CommentMapper, CommentRepository> {

    private final CommentRepository repository;
    private final CommentMapper mapper;
    private final UserService userService;
    private final PostService postService;
    private final AuthService authService;


    public CommentService(final CommentMapper mapper, final CommentRepository repository, final UserService userService, final PostService postService, final UserRepository userRepository, final AuthService authService) {
        super(mapper, repository);
        this.mapper = mapper;
        this.repository = repository;
        this.userService = userService;
        this.postService = postService;
        this.authService = authService;
    }

    public Connection<CommentDTO> getAllComments(Pageable pageable) {
        return getAll(pageable);
    }

    public CommentDTO getCommentById(UUID id) {
        return getById(id, "Comment not found");
    }

    @Transactional
    public MessageResponse deleteById(UUID id, String username) {
        authService.checkAuthorization(id, username, "User not authorized to delete comment");
        return delete(id, "Comment not found", "Comment deleted successfully!");
    }

    public MessageResponse updateComment(SimpleCommentDTO request, String username) {
        final UUID userId = userService.getUserIdByUsername(request.getUsername());
        authService.checkAuthorization(userId, username, "User not authorized to create comment");
        getCommentEntityById(request.getId());
        User user = userService.getUserEntityById(userId);
        Post post = postService.getPostEntityById(request.getPostId());
        repository.save(mapper.toEntity(request, user, post));
        return MessageResponse.builder()
                .message("Comment updated successfully!")
                .timestamp(Instant.now())
                .build();
    }

    public EntityResponse<CommentDTO> createComment(CommentInput request, String username) {
        final User user = userService.getUserEntityById(request.getUserId());
        final Post post = postService.getPostEntityById(request.getPostId());
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
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    }


}
