package com.mousty.gymbro.service;

import com.mousty.gymbro.aws.S3Service;
import com.mousty.gymbro.dto.post.PostAddDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.PostException;
import com.mousty.gymbro.mapper.PostMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.PostRepository;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostMapper mapper;
    @Mock private PostRepository repository;
    @Mock private S3Service s3Service;
    @Mock private UserService userService;

    private PostService service;

    private User author;
    private Post post;
    private PostDTO postDTO;

    @BeforeEach
    void setUp() {
        service = new PostService(mapper, repository, s3Service, userService);
        author = User.builder().id(UUID.randomUUID()).username("alice").build();
        post = Post.builder().id(UUID.randomUUID()).user(author).content("original").build();
        postDTO = PostDTO.builder().id(post.getId()).content("original").build();
    }

    @Nested
    @DisplayName("getAllPosts")
    class GetAllPosts {

        @Test
        @DisplayName("maps the page into a Connection with page info and total count")
        void mapsPage() {
            Pageable pageable = PageRequest.of(0, 1);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(post), pageable, 3));
            when(mapper.toDTO(post)).thenReturn(postDTO);

            Connection<PostDTO> result = service.getAllPosts(pageable);

            assertThat(result.results()).containsExactly(postDTO);
            assertThat(result.totalCount()).isEqualTo(3L);
            assertThat(result.pageInfo().hasNext()).isTrue();
            assertThat(result.pageInfo().hasPrevious()).isFalse();
            assertThat(result.pageInfo().totalPages()).isEqualTo(3);
            assertThat(result.pageInfo().numberOfElements()).isEqualTo(1);
            assertThat(result.pageInfo().currentPage()).isZero();
        }

        @Test
        @DisplayName("returns an empty Connection when there are no posts")
        void empty() {
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Connection<PostDTO> result = service.getAllPosts(pageable);

            assertThat(result.results()).isEmpty();
            assertThat(result.totalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getAllUserPosts")
    class GetAllUserPosts {

        @Test
        @DisplayName("returns the mapped posts of the given user")
        void returnsUserPosts() {
            when(repository.findAllByUser_Username("alice")).thenReturn(List.of(post));
            when(mapper.toDTO(post)).thenReturn(postDTO);

            assertThat(service.getAllUserPosts("alice")).containsExactly(postDTO);
        }

        @Test
        @DisplayName("returns an empty list when the user has no posts")
        void empty() {
            when(repository.findAllByUser_Username("bob")).thenReturn(List.of());

            assertThat(service.getAllUserPosts("bob")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPostById / getPostEntityById")
    class GetById {

        @Test
        @DisplayName("getPostById returns the mapped DTO")
        void dto() {
            when(repository.findById(post.getId())).thenReturn(Optional.of(post));
            when(mapper.toDTO(post)).thenReturn(postDTO);

            assertThat(service.getPostById(post.getId())).isEqualTo(postDTO);
        }

        @Test
        @DisplayName("getPostById throws PostException when not found")
        void dtoNotFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPostById(id))
                    .isInstanceOf(PostException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("getPostEntityById returns the stored entity")
        void entity() {
            when(repository.findById(post.getId())).thenReturn(Optional.of(post));

            assertThat(service.getPostEntityById(post.getId())).isSameAs(post);
        }

        @Test
        @DisplayName("getPostEntityById throws PostException when not found")
        void entityNotFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPostEntityById(id)).isInstanceOf(PostException.class);
        }
    }

    @Nested
    @DisplayName("deletePostById")
    class DeletePostById {

        @Test
        @DisplayName("the author can delete the post")
        void authorDeletes() {
            when(repository.findById(post.getId())).thenReturn(Optional.of(post));

            MessageResponse response = service.deletePostById(post.getId(), "alice");

            verify(repository).delete(post);
            assertThat(response.message()).isEqualTo("Post deleted!");
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("another user is forbidden and nothing is deleted")
        void otherUserForbidden() {
            when(repository.findById(post.getId())).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> service.deletePostById(post.getId(), "mallory"))
                    .isInstanceOf(PostException.class)
                    .extracting(e -> ((PostException) e).getStatus().value()).isEqualTo(403);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("a null username is forbidden and nothing is deleted")
        void nullUsernameForbidden() {
            when(repository.findById(post.getId())).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> service.deletePostById(post.getId(), null))
                    .isInstanceOf(PostException.class);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("throws PostException when the post does not exist")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePostById(id, "alice")).isInstanceOf(PostException.class);
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("the author can update the content; the author is unchanged")
        void authorUpdates() {
            when(repository.findById(post.getId())).thenReturn(Optional.of(post));

            MessageResponse response = service.updatePost(post.getId(), "edited", "alice");

            ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
            verify(repository).save(saved.capture());
            assertThat(saved.getValue().getContent()).isEqualTo("edited");
            assertThat(saved.getValue().getUser()).isSameAs(author);
            assertThat(response.message()).isEqualTo("Post updated successfully!");
        }

        @Test
        @DisplayName("another user is forbidden and the post is neither changed nor saved")
        void otherUserForbidden() {
            when(repository.findById(post.getId())).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> service.updatePost(post.getId(), "hacked", "mallory"))
                    .isInstanceOf(PostException.class);
            assertThat(post.getContent()).isEqualTo("original");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("throws PostException when the post does not exist")
        void notFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePost(id, "x", "alice")).isInstanceOf(PostException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        private PostAddDTO request;

        @BeforeEach
        void setUp() {
            request = PostAddDTO.builder().userId(author.getId()).content("hello").build();
        }

        @Test
        @DisplayName("creates the post for the user resolved from the username, without image")
        void withoutImage() {
            Post newPost = Post.builder().user(author).content("hello").build();
            when(userService.getUserEntityByUsername("alice")).thenReturn(author);
            when(mapper.toNewEntity(request, author)).thenReturn(newPost);
            when(repository.save(newPost)).thenReturn(post);
            when(mapper.toDTO(post)).thenReturn(postDTO);

            EntityResponse<PostDTO> response = service.createPost(request, null, "alice");

            verify(mapper).toNewEntity(request, author);
            verify(repository).save(newPost);
            verifyNoInteractions(s3Service);
            assertThat(response.result()).isEqualTo(postDTO);
            assertThat(response.message()).isEqualTo("Post added successfully!");
        }

        @Test
        @DisplayName("an empty image file is treated as no image")
        void emptyImageIgnored() {
            when(userService.getUserEntityByUsername("alice")).thenReturn(author);
            when(mapper.toNewEntity(request, author)).thenReturn(post);
            when(repository.save(post)).thenReturn(post);
            when(mapper.toDTO(post)).thenReturn(postDTO);

            service.createPost(request, new MockMultipartFile("image", new byte[0]), "alice");

            verifyNoInteractions(s3Service);
        }

        @Test
        @DisplayName("uploads the image when present and returns the presigned URL")
        void withImage() {
            MockMultipartFile image = new MockMultipartFile("image", "pic.png", "image/png", new byte[]{1});
            when(userService.getUserEntityByUsername("alice")).thenReturn(author);
            when(mapper.toNewEntity(request, author)).thenReturn(post);
            when(repository.save(post)).thenReturn(post);
            when(mapper.toDTO(post)).thenReturn(postDTO);
            when(s3Service.generatePresignedUrl(anyString())).thenReturn("https://signed");

            EntityResponse<PostDTO> response = service.createPost(request, image, "alice");

            assertThat(response.result().image()).isEqualTo("https://signed");
            assertThat(post.getImageUrl()).startsWith("users/alice/posts/post-").endsWith(".png");
            verify(s3Service).uploadFile(image, post.getImageUrl());
            verify(repository, times(2)).save(post);
        }

        @Test
        @DisplayName("rejects a request whose userId is not the caller's id; nothing is saved")
        void userIdMismatch() {
            PostAddDTO forged = request.toBuilder().userId(UUID.randomUUID()).build();
            when(userService.getUserEntityByUsername("alice")).thenReturn(author);

            assertThatThrownBy(() -> service.createPost(forged, null, "alice"))
                    .isInstanceOf(PostException.class)
                    .hasMessage("User ID mismatch");
            verify(repository, never()).save(any());
            verifyNoInteractions(s3Service);
        }
    }

    @Nested
    @DisplayName("uploadPostImage")
    class UploadPostImage {

        @Test
        @DisplayName("uploads under the user's key, stores the key and returns the presigned URL")
        void uploads() {
            MockMultipartFile image = new MockMultipartFile("image", "pic.jpg", "image/jpeg", new byte[]{1});
            when(mapper.toDTO(post)).thenReturn(postDTO);
            when(s3Service.generatePresignedUrl(anyString())).thenReturn("https://signed");

            PostDTO result = service.uploadPostImage(image, post, "alice");

            assertThat(post.getImageUrl()).matches("users/alice/posts/post-\\d+\\.jpg");
            verify(s3Service).uploadFile(image, post.getImageUrl());
            verify(s3Service).generatePresignedUrl(post.getImageUrl());
            verify(repository).save(post);
            assertThat(result.image()).isEqualTo("https://signed");
        }

        @Test
        @DisplayName("rejects a null file")
        void nullFile() {
            assertThatThrownBy(() -> service.uploadPostImage(null, post, "alice"))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(s3Service);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("rejects an empty file")
        void emptyFile() {
            MockMultipartFile empty = new MockMultipartFile("image", "x.png", "image/png", new byte[0]);

            assertThatThrownBy(() -> service.uploadPostImage(empty, post, "alice"))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(s3Service);
        }
    }
}
