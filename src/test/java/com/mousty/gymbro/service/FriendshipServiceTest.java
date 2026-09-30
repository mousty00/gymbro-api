package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.friendship.FriendshipDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.entity.Friendship;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.exception.FriendshipException;
import com.mousty.gymbro.mapper.FriendshipMapper;
import com.mousty.gymbro.mapper.PostMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.FriendshipRepository;
import com.mousty.gymbro.repository.PostRepository;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock private FriendshipMapper mapper;
    @Mock private FriendshipRepository repository;
    @Mock private AuthService authService;
    @Mock private PostRepository postRepository;
    @Mock private PostMapper postMapper;

    private FriendshipService service;

    private User requester;
    private User recipient;
    private Friendship friendship;
    private FriendshipDTO friendshipDTO;

    @BeforeEach
    void setUp() {
        service = new FriendshipService(mapper, repository, authService, postRepository, postMapper);
        requester = User.builder().id(UUID.randomUUID()).username("alice").build();
        recipient = User.builder().id(UUID.randomUUID()).username("bob").build();
        friendship = Friendship.builder().id(UUID.randomUUID())
                .user(requester).friend(recipient).status("pending").build();
        friendshipDTO = FriendshipDTO.builder().id(friendship.getId())
                .user(SimpleUserDTO.builder().username("alice").build())
                .friend(SimpleUserDTO.builder().username("bob").build())
                .status("accepted").build();
    }

    private void stubFound() {
        when(repository.findById(friendship.getId())).thenReturn(Optional.of(friendship));
    }

    private UUID stubMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        return id;
    }

    @Nested
    @DisplayName("findFriendsByUsername / getAllFriendsList")
    class FriendLists {

        @Test
        @DisplayName("findFriendsByUsername returns accepted friendships on either side, mapped")
        void findFriends() {
            when(repository.findAllInvolving("alice", "accepted")).thenReturn(List.of(friendship));
            when(mapper.toDTO(friendship)).thenReturn(friendshipDTO);

            assertThat(service.findFriendsByUsername("alice")).containsExactly(friendshipDTO);
        }

        @Test
        @DisplayName("getAllFriendsList returns only accepted friendships, mapped")
        void allFriendsList() {
            when(repository.findAllInvolving("alice", "accepted")).thenReturn(List.of(friendship));
            when(mapper.toDTO(friendship)).thenReturn(friendshipDTO);

            assertThat(service.getAllFriendsList("alice")).containsExactly(friendshipDTO);
        }

        @Test
        @DisplayName("the recipient of an accepted request sees it too (friendship is mutual)")
        void recipientSeesFriendship() {
            when(repository.findAllInvolving("bob", "accepted")).thenReturn(List.of(friendship));
            when(mapper.toDTO(friendship)).thenReturn(friendshipDTO);

            assertThat(service.getAllFriendsList("bob")).containsExactly(friendshipDTO);
        }

        @Test
        @DisplayName("returns an empty list when the user has no friends")
        void empty() {
            when(repository.findAllInvolving("carol", "accepted")).thenReturn(List.of());

            assertThat(service.getAllFriendsList("carol")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFriendshipById")
    class GetFriendshipById {

        @Test
        @DisplayName("either party can read it")
        void partiesCanRead() {
            stubFound();
            when(mapper.toDTO(friendship)).thenReturn(friendshipDTO);

            assertThat(service.getFriendshipById(friendship.getId(), "alice")).isEqualTo(friendshipDTO);
            assertThat(service.getFriendshipById(friendship.getId(), "bob")).isEqualTo(friendshipDTO);
        }

        @Test
        @DisplayName("an outsider or anonymous caller gets not found")
        void outsiderNotFound() {
            stubFound();

            assertThatThrownBy(() -> service.getFriendshipById(friendship.getId(), "mallory"))
                    .isInstanceOf(FriendshipException.class)
                    .extracting(e -> ((FriendshipException) e).getStatus().value()).isEqualTo(404);
            assertThatThrownBy(() -> service.getFriendshipById(friendship.getId(), null))
                    .isInstanceOf(FriendshipException.class);
        }

        @Test
        @DisplayName("throws FriendshipException when not found")
        void notFound() {
            UUID id = stubMissing();

            assertThatThrownBy(() -> service.getFriendshipById(id, "alice"))
                    .isInstanceOf(FriendshipException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    @DisplayName("getPendingRequests")
    class GetPendingRequests {

        @Test
        @DisplayName("returns pending requests the caller received")
        void incoming() {
            FriendshipDTO pendingDTO = friendshipDTO.toBuilder().status("pending").build();
            when(repository.findAllByFriend_UsernameAndStatus("bob", "pending")).thenReturn(List.of(friendship));
            when(mapper.toDTO(friendship)).thenReturn(pendingDTO);

            assertThat(service.getPendingRequests("bob")).containsExactly(pendingDTO);
        }
    }

    @Nested
    @DisplayName("getAllFriendships")
    class GetAllFriendships {

        @Test
        @DisplayName("queries accepted friendships of the user and builds the Connection")
        void pageOfFriends() {
            Pageable pageable = PageRequest.of(1, 1);
            when(repository.findAllInvolving("alice", "accepted", pageable))
                    .thenReturn(new PageImpl<>(List.of(friendship), pageable, 3));
            when(mapper.toDTO(friendship)).thenReturn(friendshipDTO);

            Connection<FriendshipDTO> result = service.getAllFriendships(pageable, "alice");

            assertThat(result.results()).containsExactly(friendshipDTO);
            assertThat(result.totalCount()).isEqualTo(3L);
            assertThat(result.pageInfo().currentPage()).isEqualTo(1);
            assertThat(result.pageInfo().hasNext()).isTrue();
            assertThat(result.pageInfo().hasPrevious()).isTrue();
            assertThat(result.pageInfo().totalPages()).isEqualTo(3);
            assertThat(result.pageInfo().numberOfElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("addFriend")
    class AddFriend {

        @Test
        @DisplayName("saves a new request from the caller to the given friend")
        void savesRequest() {
            Friendship newRequest = Friendship.builder().user(requester).friend(recipient).status("pending").build();
            when(mapper.toNewEntity("alice", "bob")).thenReturn(newRequest);

            MessageResponse response = service.addFriend("bob", "alice");

            verify(mapper).toNewEntity("alice", "bob");
            verify(repository).save(newRequest);
            assertThat(response.message()).isEqualTo("Friend request sent!");
        }

        @Test
        @DisplayName("sending a request to yourself is rejected")
        void self() {
            assertThatThrownBy(() -> service.addFriend("alice", "alice"))
                    .isInstanceOf(FriendshipException.class)
                    .extracting(e -> ((FriendshipException) e).getStatus().value()).isEqualTo(400);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("a request already existing in either direction is rejected")
        void duplicate() {
            when(repository.existsBetween("alice", "bob")).thenReturn(true);

            assertThatThrownBy(() -> service.addFriend("bob", "alice"))
                    .isInstanceOf(FriendshipException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("acceptFriend")
    class AcceptFriend {

        @Test
        @DisplayName("the recipient accepts: authorized against friendship.getFriend(), status becomes accepted")
        void recipientAccepts() {
            stubFound();

            MessageResponse response = service.acceptFriend(friendship.getId(), "bob");

            verify(authService).checkAuthorization(eq(recipient), eq("bob"), anyString());
            ArgumentCaptor<Friendship> saved = ArgumentCaptor.forClass(Friendship.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo("accepted");
            assertThat(response.message()).isEqualTo("Friend request accepted!");
        }

        @Test
        @DisplayName("the requester is forbidden: nothing saved, status unchanged")
        void requesterForbidden() {
            stubFound();
            doThrow(AuthException.forbidden("x"))
                    .when(authService).checkAuthorization(eq(recipient), eq("alice"), anyString());

            assertThatThrownBy(() -> service.acceptFriend(friendship.getId(), "alice"))
                    .isInstanceOf(AuthException.class);
            verify(repository, never()).save(any());
            assertThat(friendship.getStatus()).isEqualTo("pending");
        }

        @Test
        @DisplayName("a non-pending friendship (e.g. blocked) cannot be accepted")
        void notPending() {
            friendship.setStatus("blocked");
            stubFound();

            assertThatThrownBy(() -> service.acceptFriend(friendship.getId(), "bob"))
                    .isInstanceOf(FriendshipException.class);
            verify(repository, never()).save(any());
            assertThat(friendship.getStatus()).isEqualTo("blocked");
        }

        @Test
        @DisplayName("throws FriendshipException when not found")
        void notFound() {
            UUID id = stubMissing();

            assertThatThrownBy(() -> service.acceptFriend(id, "bob")).isInstanceOf(FriendshipException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("rejectFriend")
    class RejectFriend {

        @Test
        @DisplayName("the recipient rejects: authorized against friendship.getFriend(), friendship deleted")
        void recipientRejects() {
            stubFound();

            MessageResponse response = service.rejectFriend(friendship.getId(), "bob");

            verify(authService).checkAuthorization(eq(recipient), eq("bob"), anyString());
            verify(repository).delete(friendship);
            assertThat(response.message()).isEqualTo("Friend request rejected!");
        }

        @Test
        @DisplayName("the requester is forbidden: nothing deleted")
        void requesterForbidden() {
            stubFound();
            doThrow(AuthException.forbidden("x"))
                    .when(authService).checkAuthorization(eq(recipient), eq("alice"), anyString());

            assertThatThrownBy(() -> service.rejectFriend(friendship.getId(), "alice"))
                    .isInstanceOf(AuthException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("throws FriendshipException when not found")
        void notFound() {
            UUID id = stubMissing();

            assertThatThrownBy(() -> service.rejectFriend(id, "bob")).isInstanceOf(FriendshipException.class);
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("blockFriend")
    class BlockFriend {

        @Test
        @DisplayName("the requester can block")
        void requesterBlocks() {
            stubFound();

            MessageResponse response = service.blockFriend(friendship.getId(), "alice");

            ArgumentCaptor<Friendship> saved = ArgumentCaptor.forClass(Friendship.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo("blocked");
            assertThat(response.message()).isEqualTo("User blocked!");
        }

        @Test
        @DisplayName("the recipient can block")
        void recipientBlocks() {
            stubFound();

            service.blockFriend(friendship.getId(), "bob");

            verify(repository).save(friendship);
            assertThat(friendship.getStatus()).isEqualTo("blocked");
        }

        @Test
        @DisplayName("a third user is unauthorized: nothing saved, status unchanged")
        void thirdUserUnauthorized() {
            stubFound();

            assertThatThrownBy(() -> service.blockFriend(friendship.getId(), "mallory"))
                    .isInstanceOf(FriendshipException.class)
                    .extracting(e -> ((FriendshipException) e).getStatus().value()).isEqualTo(403);
            verify(repository, never()).save(any());
            assertThat(friendship.getStatus()).isEqualTo("pending");
        }

        @Test
        @DisplayName("a null username is unauthorized: nothing saved")
        void nullUsernameUnauthorized() {
            stubFound();

            assertThatThrownBy(() -> service.blockFriend(friendship.getId(), null))
                    .isInstanceOf(FriendshipException.class)
                    .hasMessageContaining("Not authorized");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws FriendshipException when not found")
        void notFound() {
            UUID id = stubMissing();

            assertThatThrownBy(() -> service.blockFriend(id, "alice"))
                    .isInstanceOf(FriendshipException.class)
                    .hasMessageContaining(id.toString());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllFriendsPosts")
    class GetAllFriendsPosts {

        @Test
        @DisplayName("returns an empty list when the user has no friends")
        void noFriends() {
            when(repository.findAllInvolving("alice", "accepted")).thenReturn(List.of());

            assertThat(service.getAllFriendsPosts("alice")).isEmpty();
            verify(postRepository, never()).findAllByUser_Username(anyString());
        }

        @Test
        @DisplayName("returns the posts of each accepted friend, not the caller's own posts")
        void returnsFriendsPosts() {
            Post bobPost = Post.builder().id(UUID.randomUUID()).user(recipient).content("bob's").build();
            PostDTO bobPostDTO = PostDTO.builder().id(bobPost.getId()).content("bob's").build();
            when(repository.findAllInvolving("alice", "accepted")).thenReturn(List.of(friendship));
            when(mapper.toDTO(friendship)).thenReturn(friendshipDTO);
            lenient().when(postRepository.findAllByUser_Username("alice")).thenReturn(List.of());
            when(postRepository.findAllByUser_Username("bob")).thenReturn(List.of(bobPost));
            when(postMapper.toDTO(bobPost)).thenReturn(bobPostDTO);

            assertThat(service.getAllFriendsPosts("alice")).containsExactly(bobPostDTO);
        }

        @Test
        @DisplayName("works from the recipient side too: bob gets alice's posts")
        void recipientSide() {
            Post alicePost = Post.builder().id(UUID.randomUUID()).user(requester).content("alice's").build();
            PostDTO alicePostDTO = PostDTO.builder().id(alicePost.getId()).content("alice's").build();
            when(repository.findAllInvolving("bob", "accepted")).thenReturn(List.of(friendship));
            when(mapper.toDTO(friendship)).thenReturn(friendshipDTO);
            when(postRepository.findAllByUser_Username("alice")).thenReturn(List.of(alicePost));
            when(postMapper.toDTO(alicePost)).thenReturn(alicePostDTO);

            assertThat(service.getAllFriendsPosts("bob")).containsExactly(alicePostDTO);
            verify(postRepository, never()).findAllByUser_Username("bob");
        }
    }
}
