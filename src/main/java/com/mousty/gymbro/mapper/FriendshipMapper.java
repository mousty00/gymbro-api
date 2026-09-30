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
                .id(dto.id())
                .user(userService.getUserEntityById(dto.user().id()))
                .friend(userService.getUserEntityById(dto.friend().id()))
                .status(dto.status())
                .createdAt(dto.createdAt())
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
