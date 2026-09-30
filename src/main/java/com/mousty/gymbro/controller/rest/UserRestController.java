package com.mousty.gymbro.controller.rest;

import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import com.mousty.gymbro.security.CurrentUsername;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserRestController {

    private final UserService userService;

    @Operation(summary = "Get all users", description = "Returns a paginated list of all users")
    @GetMapping
    public Connection<UserDTO> getAllUsers(@PageableDefault Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @Operation(summary = "get all users by username search term", description = "return a list of searched users by username")
    @GetMapping("/search")
    public List<SimpleUserDTO> searchAllByUsername(@RequestParam String username) {
        return userService.searchAllByUsername(username);
    }

    @Operation(summary = "Get user by ID", description = "Returns a single user by their UUID")
    @GetMapping("{id}")
    public UserDTO getUserById(
            @Parameter(description = "User UUID")
            @PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @Operation(summary = "Get user by username", description = "Returns a single user by their username")
    @GetMapping("/username/{username}")
    public UserDTO getUserByUsername(
            @Parameter(description = "Username")
            @PathVariable @NotBlank String username) {
        return userService.getPublicUserByUsername(username);
    }

    @Operation(summary = "Delete my account", description = "Deletes the authenticated user's own account")
    @DeleteMapping("/me")
    public MessageResponse deleteCurrentUser(
            @Parameter(hidden = true) @CurrentUsername String username) {
        return userService.deleteCurrentUser(username);
    }

    @Operation(summary = "Update user profile image", description = "Updates an existing user's profile image")
    @PutMapping(value = "/update-profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponse uploadUserImage(
            @Parameter(description = "profile image file")
            @RequestParam("imageFile") MultipartFile imageFile,
            @Parameter(description = "Current user")
            @CurrentUsername String username
    ) {
        return userService.updateUserImage(username, imageFile);
    }

}
