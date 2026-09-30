package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.post_like.LikeDTO;
import com.mousty.gymbro.dto.post_like.LikeInput;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.entity.PostLike;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.exception.LikeException;
import com.mousty.gymbro.mapper.LikeMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.LikeRepository;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock private LikeMapper mapper;
    @Mock private LikeRepository repository;
    @Mock private UserService userService;
    @Mock private PostService postService;
    @Mock private AuthService authService;

    private LikeService service;

    private User liker;
    private Post post;
    private PostLike like;
    private LikeDTO likeDTO;

    @BeforeEach
    void setUp() {
        service = new LikeService(mapper, repository, userService, postService, authService);
        liker = User.builder().id(UUID.randomUUID()).username("alice").build();
        post = Post.builder().id(UUID.randomUUID()).user(liker).content("post").build();
        like = PostLike.builder().id(UUID.randomUUID()).user(liker).post(post).build();
        likeDTO = LikeDTO.builder().id(like.getId()).postId(post.getId()).build();
    }

    @Nested
    @DisplayName("getAllLikes")
    class GetAllLikes {

        @Test
        @DisplayName("maps the page into a Connection")
        void mapsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(like), pageable, 1));
            when(mapper.toDTO(like)).thenReturn(likeDTO);

            Connection<LikeDTO> result = service.getAllLikes(pageable);

            assertThat(result.results()).containsExactly(likeDTO);
            assertThat(result.totalCount()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getLikeById")
    class GetLikeById {

        @Test
        @DisplayName("returns the mapped DTO")
        void found() {
            when(repository.findById(like.getId())).thenReturn(Optional.of(like));
            when(mapper.toDTO(like)).thenReturn(likeDTO);

            assertThat(service.getLikeById(like.getId())).isEqualTo(likeDTO);
        }

        @Test
        @DisplayName("throws LikeException when not found")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLikeById(id))
                    .isInstanceOf(LikeException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    @DisplayName("deleteLikeById")
    class DeleteLikeById {

        @Test
        @DisplayName("authorizes against the stored like owner and deletes")
        void ownerDeletes() {
            when(repository.findById(like.getId())).thenReturn(Optional.of(like));

            MessageResponse response = service.deleteLikeById(like.getId(), "alice");

            verify(authService).checkAuthorization(eq(liker), eq("alice"), anyString());
            verify(repository).delete(like);
            assertThat(response.message()).isEqualTo("Like deleted successfully!");
        }

        @Test
        @DisplayName("forbidden caller: nothing is deleted")
        void forbidden() {
            when(repository.findById(like.getId())).thenReturn(Optional.of(like));
            doThrow(AuthException.forbidden("x"))
                    .when(authService).checkAuthorization(eq(liker), eq("mallory"), anyString());

            assertThatThrownBy(() -> service.deleteLikeById(like.getId(), "mallory"))
                    .isInstanceOf(AuthException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("throws LikeException when not found; nothing is deleted")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteLikeById(id, "alice")).isInstanceOf(LikeException.class);
            verify(repository, never()).delete(any());
            verifyNoInteractions(authService);
        }
    }

    @Nested
    @DisplayName("createLike")
    class CreateLike {

        private LikeInput request;

        @BeforeEach
        void setUp() {
            request = LikeInput.builder().userId(liker.getId()).postId(post.getId()).build();
        }

        @Test
        @DisplayName("authorizes the request's userId against the caller and saves the like for that user and post")
        void createsLike() {
            PostLike newLike = PostLike.builder().user(liker).post(post).build();
            when(userService.getUserEntityById(liker.getId())).thenReturn(liker);
            when(postService.getPostEntityById(post.getId())).thenReturn(post);
            when(mapper.toNewEntity(liker, post)).thenReturn(newLike);
            when(repository.save(newLike)).thenReturn(like);
            when(mapper.toDTO(like)).thenReturn(likeDTO);

            LikeDTO result = service.createLike(request, "alice");

            verify(authService).checkAuthorization(eq(liker.getId()), eq("alice"), anyString());
            verify(mapper).toNewEntity(liker, post);
            verify(repository).save(newLike);
            assertThat(result).isEqualTo(likeDTO);
        }

        @Test
        @DisplayName("forbidden caller (liking as someone else): nothing is loaded or saved")
        void forbidden() {
            doThrow(AuthException.forbidden("x"))
                    .when(authService).checkAuthorization(eq(liker.getId()), eq("mallory"), anyString());

            assertThatThrownBy(() -> service.createLike(request, "mallory")).isInstanceOf(AuthException.class);
            verify(repository, never()).save(any());
            verifyNoInteractions(userService, postService);
        }

        @Test
        @DisplayName("liking the same post twice is rejected with 409 and nothing is saved")
        void duplicate() {
            when(userService.getUserEntityById(liker.getId())).thenReturn(liker);
            when(postService.getPostEntityById(post.getId())).thenReturn(post);
            when(repository.existsByUser_IdAndPost_Id(liker.getId(), post.getId())).thenReturn(true);

            assertThatThrownBy(() -> service.createLike(request, "alice"))
                    .isInstanceOf(LikeException.class)
                    .extracting(e -> ((LikeException) e).getStatus().value()).isEqualTo(409);
            verify(repository, never()).save(any());
        }
    }
}
