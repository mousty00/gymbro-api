package com.mousty.gymbro.mapper;

import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.entity.PostLike;
import com.mousty.gymbro.dto.post_like.LikeDTO;
import com.mousty.gymbro.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class LikeMapper implements GenericMapper<PostLike, LikeDTO> {
    private final UserMapper userMapper;

    @Override
    public LikeDTO toDTO(final PostLike postLike) {
        return LikeDTO.builder()
                .id(postLike.getId())
                .user(userMapper.toDTO(postLike.getUser()))
                .postId(postLike.getPost().getId())
                .createdAt(postLike.getCreatedAt())
                .build();
    }

    @Override
    public PostLike toEntity(final LikeDTO dto) {
        return PostLike.builder()
                .id(dto.getId())
                .user(userMapper.toEntity(dto.getUser()))
                .post(Post.builder().id(dto.getPostId()).build())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    public PostLike toNewEntity(User user, Post post) {
        return PostLike.builder()
                .user(user)
                .post(post)
                .createdAt(Instant.now())
                .build();
    }
}
