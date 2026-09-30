package com.mousty.gymbro.service;

import com.mousty.gymbro.dto.post_comment.CommentDTO;
import com.mousty.gymbro.dto.post_comment.CommentInput;
import com.mousty.gymbro.dto.post_comment.SimpleCommentDTO;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.entity.PostComment;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.exception.CommentException;
import com.mousty.gymbro.exception.PostException;
import com.mousty.gymbro.mapper.CommentMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.CommentRepository;
import com.mousty.gymbro.response.EntityResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentMapper mapper;
    @Mock private CommentRepository repository;
    @Mock private UserService userService;
    @Mock private PostService postService;
    @Mock private AuthService authService;

    private CommentService service;

    private User author;
    private Post post;
    private PostComment comment;
    private CommentDTO commentDTO;

    @BeforeEach
    void setUp() {
        service = new CommentService(mapper, repository, userService, postService, authService);
        author = User.builder().id(UUID.randomUUID()).username("alice").build();
        post = Post.builder().id(UUID.randomUUID()).user(author).content("post").build();
        comment = PostComment.builder().id(UUID.randomUUID()).user(author).post(post).content("original").build();
        commentDTO = CommentDTO.builder().id(comment.getId()).postId(post.getId()).content("original").build();
    }

    @Nested
    @DisplayName("getAllComments")
    class GetAllComments {

        @Test
        @DisplayName("maps the page into a Connection")
        void mapsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
            when(mapper.toDTO(comment)).thenReturn(commentDTO);

            Connection<CommentDTO> result = service.getAllComments(pageable);

            assertThat(result.results()).containsExactly(commentDTO);
            assertThat(result.totalCount()).isEqualTo(1L);
            assertThat(result.pageInfo().hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCommentById / getCommentEntityById")
    class GetById {

        @Test
        @DisplayName("getCommentById returns the mapped DTO")
        void dto() {
            when(repository.findById(comment.getId())).thenReturn(Optional.of(comment));
            when(mapper.toDTO(comment)).thenReturn(commentDTO);

            assertThat(service.getCommentById(comment.getId())).isEqualTo(commentDTO);
        }

        @Test
        @DisplayName("getCommentById throws CommentException when not found")
        void dtoNotFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCommentById(id))
                    .isInstanceOf(CommentException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("getCommentEntityById returns the stored entity")
        void entity() {
            when(repository.findById(comment.getId())).thenReturn(Optional.of(comment));

            assertThat(service.getCommentEntityById(comment.getId())).isSameAs(comment);
        }

        @Test
        @DisplayName("getCommentEntityById throws CommentException when not found")
        void entityNotFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCommentEntityById(id)).isInstanceOf(CommentException.class);
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("authorizes against the stored comment author and deletes")
        void authorDeletes() {
            when(repository.findById(comment.getId())).thenReturn(Optional.of(comment));

            MessageResponse response = service.deleteById(comment.getId(), "alice");

            verify(authService).checkAuthorization(eq(author), eq("alice"), anyString());
            verify(repository).delete(comment);
            assertThat(response.message()).isEqualTo("Comment deleted successfully!");
        }

        @Test
        @DisplayName("forbidden caller: nothing is deleted")
        void forbidden() {
            when(repository.findById(comment.getId())).thenReturn(Optional.of(comment));
            doThrow(AuthException.forbidden("x"))
                    .when(authService).checkAuthorization(eq(author), eq("mallory"), anyString());

            assertThatThrownBy(() -> service.deleteById(comment.getId(), "mallory"))
                    .isInstanceOf(AuthException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("throws CommentException when not found; nothing is deleted")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteById(id, "alice")).isInstanceOf(CommentException.class);
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @Test
        @DisplayName("authorizes against the STORED author, not the username in the request")
        void authorizesAgainstStoredAuthor() {
            SimpleCommentDTO request = SimpleCommentDTO.builder()
                    .id(comment.getId()).username("mallory").content("edited").build();
            when(repository.findById(comment.getId())).thenReturn(Optional.of(comment));

            service.updateComment(request, "alice");

            verify(authService).checkAuthorization(eq(author), eq("alice"), anyString());
        }

        @Test
        @DisplayName("only the content changes; author and post of the saved entity are unchanged")
        void onlyContentChanges() {
            User otherUser = User.builder().id(UUID.randomUUID()).username("mallory").build();
            SimpleCommentDTO request = SimpleCommentDTO.builder()
                    .id(comment.getId())
                    .postId(UUID.randomUUID())
                    .username(otherUser.getUsername())
                    .content("edited")
                    .build();
            when(repository.findById(comment.getId())).thenReturn(Optional.of(comment));

            MessageResponse response = service.updateComment(request, "alice");

            ArgumentCaptor<PostComment> saved = ArgumentCaptor.forClass(PostComment.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getContent()).isEqualTo("edited");
            assertThat(saved.getValue().getUser()).isSameAs(author);
            assertThat(saved.getValue().getPost()).isSameAs(post);
            assertThat(response.message()).isEqualTo("Comment updated successfully!");
        }

        @Test
        @DisplayName("forbidden caller: nothing is saved and content is unchanged")
        void forbidden() {
            SimpleCommentDTO request = SimpleCommentDTO.builder()
                    .id(comment.getId()).username("alice").content("hacked").build();
            when(repository.findById(comment.getId())).thenReturn(Optional.of(comment));
            doThrow(AuthException.forbidden("x"))
                    .when(authService).checkAuthorization(eq(author), eq("mallory"), anyString());

            assertThatThrownBy(() -> service.updateComment(request, "mallory"))
                    .isInstanceOf(AuthException.class);
            verify(repository, never()).save(any());
            assertThat(comment.getContent()).isEqualTo("original");
        }

        @Test
        @DisplayName("throws CommentException when not found; nothing is saved")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateComment(
                    SimpleCommentDTO.builder().id(id).content("x").build(), "alice"))
                    .isInstanceOf(CommentException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        private final CommentInput request = CommentInput.builder().content("nice").build();

        @Test
        @DisplayName("the author is the user resolved from the caller's username")
        void authorFromUsername() {
            CommentInput input = request.toBuilder().postId(post.getId()).build();
            PostComment newComment = PostComment.builder().user(author).post(post).content("nice").build();
            when(userService.getUserEntityByUsername("alice")).thenReturn(author);
            when(postService.getPostEntityById(post.getId())).thenReturn(post);
            when(mapper.toNewEntity(input, author, post)).thenReturn(newComment);
            when(repository.save(newComment)).thenReturn(comment);
            when(mapper.toDTO(comment)).thenReturn(commentDTO);

            EntityResponse<CommentDTO> response = service.createComment(input, "alice");

            verify(userService).getUserEntityByUsername("alice");
            verify(mapper).toNewEntity(input, author, post);
            verify(repository).save(newComment);
            assertThat(response.result()).isEqualTo(commentDTO);
            assertThat(response.message()).isEqualTo("Comment added successfully!");
        }

        @Test
        @DisplayName("unknown post: nothing is saved")
        void postNotFound() {
            UUID postId = UUID.randomUUID();
            CommentInput input = request.toBuilder().postId(postId).build();
            when(userService.getUserEntityByUsername("alice")).thenReturn(author);
            when(postService.getPostEntityById(postId)).thenThrow(PostException.notFound(postId));

            assertThatThrownBy(() -> service.createComment(input, "alice")).isInstanceOf(PostException.class);
            verify(repository, never()).save(any());
        }
    }
}
