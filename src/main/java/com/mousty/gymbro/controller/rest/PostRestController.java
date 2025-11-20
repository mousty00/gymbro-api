package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.PostService;
import com.mousty.gymbro.dto.post.PostAddDTO;
import com.mousty.gymbro.dto.post.PostDTO;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.EntityResponse;
import com.mousty.gymbro.response.MessageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Posts management APIs")
public class PostRestController {

    private final PostService service;

    @Operation(summary = "Get all posts", description = "Retrieve a paginated list of all posts")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved posts")
    @GetMapping
    public Connection<PostDTO> getAllPosts(@PageableDefault final Pageable pageable) {
        return service.getAllPosts(pageable);
    }

    @Operation(summary = "Get user posts", description = "Retrieve all posts for a specific user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user posts")
    @GetMapping("/user")
    public List<PostDTO> getAllUserPosts(@RequestParam String username) {
        return service.getAllUserPosts(username);
    }

    @Operation(summary = "Get post by ID", description = "Retrieve a specific post by its ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved post")
    @ApiResponse(responseCode = "404", description = "Post not found")
    @GetMapping("/{id}")
    public PostDTO getPostById(@PathVariable @NotNull final UUID id) {
        return service.getPostById(id);
    }


    @Operation(summary = "Delete post", description = "Delete a post by its ID")
    @ApiResponse(responseCode = "200", description = "Post successfully deleted")
    @ApiResponse(responseCode = "404", description = "Post not found")
    @DeleteMapping("/delete/{id}")
    public MessageResponse deletePostById(@PathVariable final UUID id) {
        return service.deletePostById(id);
    }

    @Operation(summary = "Create post", description = "Create a new post")
    @ApiResponse(responseCode = "201", description = "Post successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping(value="/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EntityResponse<PostDTO> createPost(
            @Valid
            @RequestPart final PostAddDTO request,
            @RequestPart MultipartFile imageFile,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.createPost(request,imageFile, username);
    }

    @Operation(summary = "Update post", description = "Update an existing post")
    @ApiResponse(responseCode = "200", description = "Post successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "404", description = "Post not found")
    @PutMapping("/update")
    public MessageResponse updatePost(
            @Valid @RequestBody final PostDTO request,
            @CurrentSecurityContext(expression = "authentication?.name")
            String username) {
        return service.updatePost(request, username);
    }

}
