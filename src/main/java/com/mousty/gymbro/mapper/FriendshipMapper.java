package com.mousty.gymbro.mapper;

import com.mousty.gymbro.entity.Friendship;
import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.dto.friendship.FriendshipDTO;
import com.mousty.gymbro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class FriendshipMapper implements GenericMapper<Friendship, FriendshipDTO> {
    private final UserMapper userMapper;
    private final UserService userService;

    @Override
    public FriendshipDTO toDTO(final Friendship friendship) {
        return FriendshipDTO.builder()
                .id(friendship.getId())
                .user(userMapper.toSimpleDTO(friendship.getUser(), userService.generateImageUrl(friendship.getUser())))
                .friend(userMapper.toSimpleDTO(friendship.getFriend(), userService.generateImageUrl(friendship.getFriend())))
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .build();
    }

    @Override
    public Friendship toEntity(final FriendshipDTO dto) {
        return Friendship.builder()
                .id(dto.getId())
                .user(userService.getUserEntityById(dto.getUser().getId()))
                .friend(userService.getUserEntityById(dto.getFriend().getId()))
                .status(dto.getStatus())
                .createdAt(dto.getCreatedAt())
                .updatedAt(null)
                .build();
    }

    public Friendship toNewEntity(String username, String friendUsername) {
        return Friendship.builder()
                .user(userService.getUserEntityByUsername(username))
                .friend(userService.getUserEntityByUsername(friendUsername))
                .status("pending")
                .createdAt(Instant.now())
                .updatedAt(null)
                .build();
    }
}
