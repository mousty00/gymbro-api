package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.LikeService;
import com.mousty.gymbro.dto.post_like.LikeDTO;
import com.mousty.gymbro.dto.post_like.LikeInput;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Tag(name = "Likes", description = "Like management endpoints")
public class LikeRestController {

    private final LikeService likeService;

    @Operation(summary = "Get all likes", description = "Returns a paginated list of all likes")
    @GetMapping
    public Connection<LikeDTO> getAll(@PageableDefault Pageable pageable) {
        return likeService.getAllLikes(pageable);
    }

    @Operation(summary = "Get like by ID", description = "Returns a like by its ID")
    @GetMapping("/{id}")
    public LikeDTO getLikeById(@PathVariable UUID id) {
        return likeService.getLikeById(id);
    }

    @Operation(summary = "Create new like", description = "Creates a new like and returns it")
    @PostMapping("/create")
    public LikeDTO createLike(
            @Valid @RequestBody LikeInput request,
            @CurrentSecurityContext(expression = "authentication?.name") String username
    ) {
        return likeService.createLike(request, username);
    }

    @Operation(summary = "Delete like", description = "Deletes a like by its ID")
    @DeleteMapping("/delete/{id}")
    public MessageResponse deleteLikeById(
            @PathVariable UUID id,
            @CurrentSecurityContext(expression = "authentication?.name") String username
    ) {
        return likeService.deleteLikeById(id, username);
    }

}
