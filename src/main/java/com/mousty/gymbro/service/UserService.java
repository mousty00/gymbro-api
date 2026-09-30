package com.mousty.gymbro.service;

import com.mousty.gymbro.aws.S3Service;
import com.mousty.gymbro.entity.Role;
import com.mousty.gymbro.exception.UserException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final S3Service s3Service;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${default.profile.image.key}")
    private String defaultProfileImageKey;

    public Connection<UserDTO> getAllUsers(final Pageable pageable) {
        final Page<User> page = repository.findAll(pageable);
        final List<UserDTO> users = page
                .map(user -> mapper.toPublicDTO(user, s3Service.generatePresignedUrl(user.getImage())))
                .toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());

        return new Connection<>(users, info, page.getTotalElements());
    }

    @Transactional
    public UserDTO getUserByUsername(String username) {
        return repository.findUserByUsername(username)
                .map(user -> mapper.toDTO(user, s3Service.generatePresignedUrl(user.getImage())))
                .orElseThrow(() -> UserException.notFound(username));
    }

    public List<SimpleUserDTO> searchAllByUsername(String username) {
        return repository.findAllByUsernameLike(username)
                .stream().map(user -> mapper.toSimpleDTO(user, s3Service.generatePresignedUrl(user.getImage())))
                .toList();
    }

    public UserDTO getPublicUserByUsername(String username) {
        return repository.findUserByUsername(username)
                .map(user -> mapper.toPublicDTO(user, s3Service.generatePresignedUrl(user.getImage())))
                .orElseThrow(() -> UserException.notFound(username));
    }

    public UserDTO getUserById(UUID id) {
        return repository.findById(id)
                .map(user -> mapper.toPublicDTO(user, s3Service.generatePresignedUrl(user.getImage())))
                .orElseThrow(() -> UserException.notFound(id));
    }

    @Transactional
    public UserDTO createUser(SignupDTO request) {
        if (repository.existsUsersByUsername(request.username())) {
            throw UserException.alreadyExists("Username", request.username());
        }
        if (repository.existsUsersByEmail(request.email())) {
            throw UserException.alreadyExists("Email", request.email());
        }
        // entity stores the S3 key; presigned URLs are generated on read and expire
        Role role = roleRepository.getRolesByName("User");
        final User user = repository.save(mapper.fromSignupDTO(request, role, defaultProfileImageKey, passwordEncoder));
        return mapper.toDTO(user, s3Service.generatePresignedUrl(defaultProfileImageKey));
    }

    // Self-service only: the account to delete comes from the bearer token, never from the client.
    @Transactional
    public MessageResponse deleteCurrentUser(String username) {
        repository.delete(getUserEntityByUsername(username));
        return MessageResponse.builder()
                .message("User deleted!")
                .timestamp(Instant.now())
                .build();
    }

    public User getUserEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> UserException.notFound(id));
    }

    public String getUsernameById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> UserException.notFound(id)).getUsername();
    }

    public UUID getUserIdByUsername(String username) {
        return repository.findUserByUsername(username)
                .orElseThrow(() -> UserException.notFound(username)).getId();
    }

    public User getUserEntityByUsername(String username) {
        return repository.findUserByUsername(username)
                .orElseThrow(() -> UserException.notFound(username));
    }

    @Transactional
    public MessageResponse updateUserImage(
            final String username,
            final MultipartFile imageFile) {
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
                .orElseThrow(() -> UserException.notFound(username));
        log.info("Updated user image key: {}", updatedUser.getImage());

        return MessageResponse.builder()
                .message("Image updated successfully")
                .timestamp(Instant.now())
                .build();
    }

    public UserDTO getUserWithImageUrl(String username) {
        User user = getUserEntityByUsername(username);
        String imageUrl = (user.getImage() != null && !user.getImage().isEmpty())
                ? s3Service.generatePresignedUrl(user.getImage())
                : null;
        return mapper.toDTO(user, imageUrl);
    }

    public String generateImageUrl(User user) {
        return s3Service.generatePresignedUrl(user.getImage());
    }
}
