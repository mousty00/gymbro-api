package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.mapper.PostMapper;
import com.mousty.gymbro.service.PostService;
import com.mousty.gymbro.dto.post.PostAddDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class PostController {

    private final PostService service;
    private final PostMapper mapper;

    @DgsQuery
    public List<PostDTO> userPosts(@InputArgument String username) {
        return service.getAllUserPosts(username);
    }

    @DgsQuery
    public PostDTO post(@InputArgument UUID id) {
        return service.getPostById(id);
    }

    @DgsMutation
    public MessageResponse deletePost(@InputArgument UUID id) {
        return service.deletePostById(id);
    }

//    @DgsMutation
//    public MessageResponse updatePost(
//            @InputArgument PostInput request,
//            @CurrentSecurityContext(expression = "authentication?.name") String username) {
//        PostDTO postDTO = mapper.fromInput(request);
//        return service.updatePost(postDTO, username);
//    }

    @DgsMutation
    public EntityResponse<PostDTO> createPost(
            @InputArgument PostAddDTO request,
            @InputArgument MultipartFile imageFile,
            @CurrentSecurityContext(expression = "authentication?.name") String username) {
        return service.createPost(request,imageFile,username);
    }
}