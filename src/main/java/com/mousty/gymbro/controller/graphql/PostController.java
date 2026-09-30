package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.dto.post.PostAddDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.dto.post.PostInput;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.service.PostService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mousty.gymbro.security.CurrentUsername;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PostController {

    private final PostService service;

    @DgsQuery
    public List<PostDTO> userPosts(@InputArgument String username) {
        return service.getAllUserPosts(username);
    }

    @DgsQuery
    public PostDTO post(@InputArgument UUID id) {
        return service.getPostById(id);
    }

    @DgsMutation
    public MessageResponse deletePost(
            @InputArgument UUID id,
            @CurrentUsername String username) {
        return service.deletePostById(id, username);
    }

    @DgsMutation
    public MessageResponse updatePost(
            @Valid @InputArgument PostInput request,
            @CurrentUsername String username) {
        return service.updatePost(request.id(), request.content(), username);
    }

    @DgsMutation
    public EntityResponse<PostDTO> createPost(
            @Valid @InputArgument PostAddDTO request,
            @InputArgument MultipartFile imageFile,
            @CurrentUsername String username) {
        return service.createPost(request, imageFile, username);
    }
}
