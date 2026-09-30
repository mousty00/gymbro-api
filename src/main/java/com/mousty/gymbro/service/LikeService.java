package com.mousty.gymbro.service;

import com.mousty.gymbro.exception.LikeException;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.mapper.LikeMapper;
import com.mousty.gymbro.repository.LikeRepository;
import com.mousty.gymbro.entity.PostLike;
import com.mousty.gymbro.dto.post_like.LikeDTO;
import com.mousty.gymbro.dto.post_like.LikeInput;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LikeService extends GenericService<PostLike, LikeDTO, LikeMapper, LikeRepository> {

    private final UserService userService;
    private final PostService postService;
    private final AuthService authService;

    public LikeService(final LikeMapper mapper, final LikeRepository repository, final UserService userService, final PostService postService, final AuthService authService) {
        super(mapper, repository);
        this.userService = userService;
        this.postService = postService;
        this.authService = authService;
    }

    public Connection<LikeDTO> getAllLikes(Pageable pageable) {
        return getAll(pageable);
    }

    @Transactional
    public MessageResponse deleteLikeById(UUID id, String username) {
        final PostLike like = repository.findById(id)
                .orElseThrow(() -> LikeException.notFound(id));
        authService.checkAuthorization(like.getUser(), username, "User not authorized to delete like");
        repository.delete(like);
        return MessageResponse.builder()
                .message("Like deleted successfully!")
                .timestamp(Instant.now())
                .build();
    }

    public LikeDTO getLikeById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> LikeException.notFound(id));
    }

    public LikeDTO createLike(LikeInput request, String username) {
        authService.checkAuthorization(request.userId(), username, "User not authorized to create like");
        final User user = userService.getUserEntityById(request.userId());
        final Post post = postService.getPostEntityById(request.postId());
        if (repository.existsByUser_IdAndPost_Id(user.getId(), post.getId())) {
            throw LikeException.duplicate();
        }
        final PostLike like = repository.save(mapper.toNewEntity(user, post));
        return mapper.toDTO(like);
    }
}
