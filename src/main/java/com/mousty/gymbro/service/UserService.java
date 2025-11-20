package com.mousty.gymbro.service;

import com.mousty.gymbro.aws.S3Service;
import com.mousty.gymbro.entity.Role;
import com.mousty.gymbro.repository.RoleRepository;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.mapper.UserMapper;
import com.mousty.gymbro.repository.UserRepository;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.response.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final S3Service s3Service;
    private final RoleRepository roleRepository;
    @Value("${default.profile.image.key}")
    private String defaultProfileImageKey;

    public UserService(final UserMapper mapper, final UserRepository repository, final S3Service s3Service, final RoleRepository roleRepository) {
        this.mapper = mapper;
        this.repository = repository;
        this.s3Service = s3Service;
        this.roleRepository = roleRepository;
    }

    public Connection<UserDTO> getAllUsers(final Pageable pageable) {
        final Page<User> page = repository.findAll(pageable);
        final List<UserDTO> users = page
                .map(mapper::toDTO)
                .toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());

        return new Connection<>(users, info, page.getTotalElements());
    }

    @Transactional
    public UserDTO getUserByUsername(String username) {
        return repository.findUserByUsername(username)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    public List<SimpleUserDTO> searchAllByUsername(String username) {
        return repository.findAllByUsernameLike(username)
                .stream().map( user -> {
                    return mapper.toSimpleDTO(user, s3Service.generatePresignedUrl(user.getImage()));
                })
                .toList();
    }

    @Transactional
    public UserDTO getUserByEmail(String email) {
        final User user = repository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return mapper.toDTO(user);
    }

    public UserDTO getUserById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    @Transactional
    public UserDTO createUser(SignupDTO request, PasswordEncoder passwordEncoder) {
        if (repository.existsUsersByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (repository.existsUsersByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Username already exists");
        }
        request.setImage(s3Service.generatePresignedUrl(defaultProfileImageKey));
        Role role = roleRepository.getRolesByName("user");
        final User user = repository.save(mapper.fromSignupDTO(request,role, passwordEncoder));
        return mapper.toDTO(user);
    }

    @Transactional
    public MessageResponse deleteUserById(UUID id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("user not found");
        }
        repository.deleteById(id);
        return MessageResponse.builder()
                .message("user deleted!")
                .timestamp(Instant.now())
                .build();
    }

//    public ResponseEntity<?> updateUser(UserInput userRequest) {
//        repository.findById(UUID.fromString(userRequest.getId()));
//        var posts = postService.getAllUserPosts(actualUser.getUsername());
//        var
//
//        final User updatedUser = repository.save(mapper.toEntity(actualUser,posts, ));
//
//        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
//
//        if (currentAuth.getName().equals(actualUser.getUsername())) {
//            Authentication newAuth = new UsernamePasswordAuthenticationToken(
//                    updatedUser.getUsername(),
//                    updatedUser.getPassword(),
//                    currentAuth.getAuthorities()
//            );
//            SecurityContextHolder.getContext().setAuthentication(newAuth);
//            final String token = jwtUtil.generateToken(mapper.toDTO(updatedUser, generateImageUrl(updatedUser)));
//
//            return ResponseEntity.ok(LoginResponse.builder()
//                    .message("User updated successfully")
//                    .token(token)
//                    .result(mapper.toDTO(updatedUser, generateImageUrl(updatedUser)))
//                    .build());
//        }
//
//        return ResponseEntity.ok(MessageResponse.builder()
//                .message("User updated successfully!")
//                .timestamp(Instant.now())
//                .build());
//    }

    public User getUserEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("user not found"));
    }

    public String getUsernameById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("user not found")).getUsername();
    }

    public UUID getUserIdByUsername(String username) {
        return repository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("user not found")).getId();
    }

    public User getUserEntityByUsername(String username) {
        return repository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("user not found"));
    }

    @Transactional
    public ResponseEntity<MessageResponse> updateUserImage(
            final String username,
            final MultipartFile imageFile) {
        try {
            User user = getUserEntityByUsername(username);
            if (imageFile == null || imageFile.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            if (user.getImage() != null) {
                try {
                    log.warn("old file deleted : {}", user.getImage());
                    s3Service.deleteFile(user.getImage());
                } catch (Exception e) {
                    log.warn("Failed to delete old image: {}", e.getMessage());
                }
            }

            String extension = FilenameUtils.getExtension(imageFile.getOriginalFilename());
            String imageKey = String.format("users/%s/profile-%d.%s",
                    username, System.currentTimeMillis(), extension);

            s3Service.uploadFile(imageFile, imageKey);

            user.setImage(imageKey);
            repository.saveAndFlush(user);

            User updatedUser = repository.findUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found after update"));
            log.info("Updated user image key: {}", updatedUser.getImage());

            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Image updated successfully")
                    .timestamp(Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("Image update failed", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Image update failed", e);
        }
    }

    public UserDTO getUserWithImageUrl(String username) {
        User user = getUserEntityByUsername(username);
        UserDTO dto = mapper.toDTO(user);

        if (user.getImage() != null && !user.getImage().isEmpty()) {
            dto.setImage(s3Service.generatePresignedUrl(user.getImage()));
        }

        return dto;
    }

    public String generateImageUrl(User user) {
        return s3Service.generatePresignedUrl(user.getImage());
    }

}
