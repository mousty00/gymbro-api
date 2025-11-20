package com.mousty.gymbro.service;

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

import java.util.UUID;

@Service
public class LikeService extends GenericService<PostLike, LikeDTO, LikeMapper, LikeRepository> {

    private final LikeMapper mapper;
    private final LikeRepository repository;
    private final UserService userService;
    private final PostService postService;
    private final AuthService authService;

    public LikeService(final LikeMapper mapper, final LikeRepository repository, final UserService userService, final PostService postService, final AuthService authService) {
        super(mapper, repository);
        this.mapper = mapper;
        this.repository = repository;
        this.userService = userService;
        this.postService = postService;
        this.authService = authService;
    }

    public Connection<LikeDTO> getAllLikes (Pageable pageable) {
        return getAll(pageable);
    }

    @Transactional
    public MessageResponse deleteLikeById(UUID id, String username) {
        return delete(id, "Like not found", "Like deleted successfully!");
    }

    public LikeDTO getLikeById(UUID id) {
        return getById(id, "Like not found");
    }

    public LikeDTO createLike(LikeInput request,String username) {
        authService.checkAuthorization(request.getUserId(), username, "User not authorized to create like");
        final User user = userService.getUserEntityById(request.getUserId());
        final Post post = postService.getPostEntityById(request.getPostId());
        final PostLike like = repository.save(mapper.toNewEntity(user, post));
        return mapper.toDTO(like);
    }
}
