package com.mousty.gymbro.service;

import com.mousty.gymbro.entity.Friendship;
import com.mousty.gymbro.exception.FriendshipException;
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
import java.util.UUID;

@Service
public class FriendshipService extends GenericService<Friendship, FriendshipDTO, FriendshipMapper, FriendshipRepository> {

    private final AuthService authService;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public FriendshipService(final FriendshipMapper mapper, final FriendshipRepository repository, final AuthService authService, final PostRepository postRepository, final PostMapper postMapper) {
        super(mapper, repository);
        this.authService = authService;
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    public List<FriendshipDTO> findFriendsByUsername(final String username) {
        return getAllFriendsList(username);
    }

    /** Only the two people in the friendship can read it; for anyone else it doesn't exist. */
    public FriendshipDTO getFriendshipById(final UUID id, final String username) {
        final Friendship friendship = getFriendshipEntityById(id);
        if (!isParty(friendship, username)) {
            throw FriendshipException.notFound(id);
        }
        return mapper.toDTO(friendship);
    }

    /** Incoming pending requests, so the recipient can accept or reject them. */
    public List<FriendshipDTO> getPendingRequests(final String username) {
        return repository.findAllByFriend_UsernameAndStatus(username, "pending")
                .stream().map(mapper::toDTO).toList();
    }

    public Connection<FriendshipDTO> getAllFriendships(Pageable pageable, String username) {
        final Page<FriendshipDTO> page = repository.findAllInvolving(username, "accepted", pageable).map(mapper::toDTO);
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
        return repository.findAllInvolving(username, "accepted")
                .stream().map(mapper::toDTO).toList();
    }

    public MessageResponse addFriend(final String friendUsername, String username) {
        if (friendUsername.equals(username)) {
            throw FriendshipException.cannotBefriendSelf();
        }
        // any existing row in either direction (pending, accepted or blocked) blocks a new request
        if (repository.existsBetween(username, friendUsername)) {
            throw FriendshipException.alreadyFriends();
        }
        repository.save(mapper.toNewEntity(username, friendUsername));
        return MessageResponse.builder()
                        .message("Friend request sent!")
                        .timestamp(Instant.now())
                        .build();
    }

    public MessageResponse acceptFriend(UUID id, String username) {
        final Friendship friendship = getFriendshipForRecipient(id, username);
        friendship.setStatus("accepted");
        repository.save(friendship);
        return MessageResponse.builder()
                .message("Friend request accepted!")
                .timestamp(Instant.now())
                .build();
    }

    public MessageResponse rejectFriend(UUID id, String username) {
        final Friendship friendship = getFriendshipForRecipient(id, username);
        repository.delete(friendship);
        return MessageResponse.builder()
                .message("Friend request rejected!")
                .timestamp(Instant.now())
                .build();
    }

    public MessageResponse blockFriend(UUID id, String username) {
        final Friendship friendship = getFriendshipEntityById(id);
        // either side of the friendship may block
        if (!isParty(friendship, username)) {
            throw FriendshipException.unauthorized();
        }
        friendship.setStatus("blocked");
        repository.save(friendship);
        return MessageResponse.builder()
                .message("User blocked!")
                .timestamp(Instant.now())
                .build();
    }

    public List<PostDTO> getAllFriendsPosts(final String username) {
        final List<FriendshipDTO> friends = getAllFriendsList(username);

        return friends.stream()
                .flatMap(friend -> postRepository.findAllByUser_Username(otherParty(friend, username))
                        .stream()
                        .map(postMapper::toDTO))
                .toList();
    }

    // Only the recipient (friend) may accept or reject, and only while the request is pending
    // (otherwise a blocked user could "accept" their way out of the block).
    private Friendship getFriendshipForRecipient(final UUID id, final String username) {
        final Friendship friendship = getFriendshipEntityById(id);
        authService.checkAuthorization(friendship.getFriend(), username, "User not authorized to modify this friendship");
        if (!"pending".equals(friendship.getStatus())) {
            throw FriendshipException.notFound(id);
        }
        return friendship;
    }

    private static String otherParty(final FriendshipDTO friendship, final String username) {
        return friendship.user().username().equals(username)
                ? friendship.friend().username()
                : friendship.user().username();
    }

    private static boolean isParty(final Friendship friendship, final String username) {
        return username != null && (friendship.getUser().getUsername().equals(username)
                || friendship.getFriend().getUsername().equals(username));
    }

    private Friendship getFriendshipEntityById(final UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> FriendshipException.notFound(id));
    }
}
