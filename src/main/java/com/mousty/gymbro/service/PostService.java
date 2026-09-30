package com.mousty.gymbro.service;

import com.mousty.gymbro.aws.S3Service;
import com.mousty.gymbro.entity.Post;
import com.mousty.gymbro.exception.PostException;
import com.mousty.gymbro.mapper.PostMapper;
import com.mousty.gymbro.repository.PostRepository;
import com.mousty.gymbro.dto.post.PostAddDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper mapper;
    private final PostRepository repository;
    private final S3Service s3Service;
    private final UserService userService;

    public Connection<PostDTO> getAllPosts(final Pageable pageable) {
        final Page<Post> page = repository.findAll(pageable);
        final List<PostDTO> posts = page
                .map(mapper::toDTO)
                .toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());

        return new Connection<>(posts, info, page.getTotalElements());
    }

    public List<PostDTO> getAllUserPosts(final String username) {
        return repository.findAllByUser_Username(username)
                .stream().map(mapper::toDTO).toList();
    }

    @Transactional
    public MessageResponse deletePostById(final UUID id, String username) {
        Post post = repository.findById(id)
                .orElseThrow(() -> PostException.notFound(id));

        if (!post.getUser().getUsername().equals(username)) {
            throw PostException.unauthorized();
        }

        repository.delete(post);
        return MessageResponse.builder()
                .message("Post deleted!")
                .timestamp(Instant.now())
                .build();
    }

    public PostDTO getPostById(final UUID id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> PostException.notFound(id));
    }

    @Transactional
    public MessageResponse updatePost(final UUID id, final String content, String username) {
        Post post = repository.findById(id)
                .orElseThrow(() -> PostException.notFound(id));

        if (!post.getUser().getUsername().equals(username)) {
            throw PostException.unauthorized();
        }

        post.setContent(content);
        repository.save(post);

        return MessageResponse.builder()
                        .message("Post updated successfully!")
                        .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public EntityResponse<PostDTO> createPost(
            final PostAddDTO request,
            final MultipartFile imageFile,
            String username) {
        User user = userService.getUserEntityByUsername(username);

        if (!user.getId().equals(request.userId())) {
            throw PostException.userIdMismatch();
        }

        final Post post = mapper.toNewEntity(request, user);
        Post savedPost = repository.save(post);

        PostDTO postDTO;
        if (imageFile != null && !imageFile.isEmpty()) {
            postDTO = uploadPostImage(imageFile, savedPost, username);
        } else {
            postDTO = mapper.toDTO(savedPost);
        }

        return EntityResponse.<PostDTO>builder()
                        .message("Post added successfully!")
                        .result(postDTO)
                        .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public PostDTO uploadPostImage(MultipartFile imageFile, Post post, String username) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        String extension = FilenameUtils.getExtension(imageFile.getOriginalFilename());
        String imageKey = String.format("users/%s/posts/post-%d.%s",
                username, System.currentTimeMillis(), extension);

        s3Service.uploadFile(imageFile, imageKey);
        post.setImageUrl(imageKey);
        repository.save(post);
        PostDTO dto = mapper.toDTO(post);
        dto = dto.toBuilder().image(s3Service.generatePresignedUrl(post.getImageUrl())).build();
        return dto;
    }

    public Post getPostEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> PostException.notFound(id));
    }
}
