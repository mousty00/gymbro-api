package com.mousty.gymbro.service;

import com.mousty.gymbro.entity.Friendship;
import com.mousty.gymbro.generic.GenericService;
import com.mousty.gymbro.mapper.FriendshipMapper;
import com.mousty.gymbro.repository.FriendshipRepository;
import com.mousty.gymbro.dto.friendship.FriendshipDTO;
import com.mousty.gymbro.mapper.PostMapper;
import com.mousty.gymbro.repository.PostRepository;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class FriendshipService extends GenericService<Friendship, FriendshipDTO, FriendshipMapper, FriendshipRepository> {

    private final FriendshipRepository repository;
    private final FriendshipMapper mapper;
    private final AuthService authService;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public FriendshipService(final FriendshipMapper mapper, final FriendshipRepository repository, final AuthService authService, final PostRepository postRepository, final PostMapper postMapper) {
        super(mapper, repository);
        this.mapper = mapper;
        this.repository = repository;
        this.authService = authService;
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    public List<FriendshipDTO> findFriendsByUsername(final String username) {
        return repository.findAllByUser_UsernameAndStatus(username, "accepted")
                .stream().map(mapper::toDTO).toList();
    }

    public FriendshipDTO getFriendshipById(final UUID id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("friend request not found"));
    }

    public Connection<FriendshipDTO> getAllFriendships(Pageable pageable, String username) {
        final Page<FriendshipDTO> page = repository.findByUser_UsernameAndStatus(username, "accepted", pageable);
        PageInfo pageInfo = PageInfo.builder()
                .currentPage(page.getNumber())
                .hasNext(page.hasNext())
                .totalPages(page.getTotalPages())
                .numberOfElements(page.getNumberOfElements())
                .hasPrevious(page.hasPrevious())
                .build();
        return Connection.<FriendshipDTO>builder()
                .results(page.getContent())
                .pageInfo(pageInfo)
                .totalCount(page.getTotalElements())
                .build();
    }

    public List<FriendshipDTO> getAllFriendsList(String username) {
        return repository.findAllByUser_UsernameAndStatus(username, "accepted")
                .stream().map(mapper::toDTO).toList();
    }

    public MessageResponse addFriend(final String friendUsername, String username) {
        repository.save(mapper.toNewEntity(friendUsername, username));
        return MessageResponse.builder()
                        .message("friend request sent!")
                        .timestamp(Instant.now())
                        .build();
    }

    public MessageResponse acceptFriend(UUID id, String username) {
        final Friendship friendship = checkUserAuthorizationAndGetFriendship(id, username);
        friendship.setStatus("accepted");
        repository.save(friendship);
        return MessageResponse.builder()
                .message("friend request accepted!")
                .timestamp(Instant.now())
                .build();
    }

    public MessageResponse rejectFriend(UUID id, String username) {
        final Friendship friendship = checkUserAuthorizationAndGetFriendship(id, username);
        repository.delete(friendship);
        return MessageResponse.builder()
                .message("friend request accepted!")
                .timestamp(Instant.now())
                .build();
    }

    public MessageResponse blockFriend(UUID id, String username) {
        final Friendship friendship = checkUserAuthorizationAndGetFriendship(id, username);
        friendship.setStatus("blocked");
        repository.save(friendship);
        return MessageResponse.builder()
                .message("friend request blocked!")
                .timestamp(Instant.now())
                .build();
    }

    public List<PostDTO> getAllFriendsPosts(final String username) {
        final List<FriendshipDTO> friends = getAllFriendsList(username);

        return friends.stream()
                .flatMap(friend -> postRepository.findAllByUser_Username(friend.getUser().getUsername())
                        .stream()
                        .map(postMapper::toDTO))
                .toList();
    }

    private Friendship checkUserAuthorizationAndGetFriendship(final UUID id, final String username) {
        final Friendship friendship = mapper.toEntity(getFriendshipById(id));
        authService.checkAuthorization(friendship.getUser().getId(), username, "user not authorized");
        return friendship;
    }

}
