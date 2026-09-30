package com.mousty.gymbro.service;

import com.mousty.gymbro.aws.S3Service;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.entity.Role;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.UserException;
import com.mousty.gymbro.mapper.UserMapper;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.repository.RoleRepository;
import com.mousty.gymbro.repository.UserRepository;
import com.mousty.gymbro.response.MessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USERNAME = "mousty";
    private static final String EMAIL = "mousty@example.com";
    private static final String DEFAULT_KEY = "defaults/profile.png";

    @Mock private UserRepository repository;
    @Mock private UserMapper mapper;
    @Mock private S3Service s3Service;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private final UserDTO userDTO = UserDTO.builder().username(USERNAME).build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "defaultProfileImageKey", DEFAULT_KEY);
        user = User.builder()
                .id(UUID.randomUUID())
                .username(USERNAME)
                .email(EMAIL)
                .image("users/mousty/old.png")
                .build();
    }

    @Nested
    @DisplayName("public projections")
    class PublicProjections {

        @Test
        @DisplayName("getAllUsers maps every user with toPublicDTO and keeps paging info")
        void getAllUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));
            when(s3Service.generatePresignedUrl(user.getImage())).thenReturn("url");
            when(mapper.toPublicDTO(user, "url")).thenReturn(userDTO);

            Connection<UserDTO> result = userService.getAllUsers(pageable);

            assertThat(result.results()).containsExactly(userDTO);
            assertThat(result.totalCount()).isEqualTo(1L);
            assertThat(result.pageInfo().hasNext()).isFalse();
            verify(mapper, never()).toDTO(any(User.class), anyString());
        }

        @Test
        @DisplayName("getUserById uses toPublicDTO")
        void getUserById() {
            when(repository.findById(user.getId())).thenReturn(Optional.of(user));
            when(s3Service.generatePresignedUrl(user.getImage())).thenReturn("url");
            when(mapper.toPublicDTO(user, "url")).thenReturn(userDTO);

            assertThat(userService.getUserById(user.getId())).isSameAs(userDTO);
            verify(mapper, never()).toDTO(any(User.class), anyString());
        }

        @Test
        @DisplayName("getUserById unknown id throws UserException")
        void getUserByIdNotFound() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(id)).isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("getPublicUserByUsername uses toPublicDTO")
        void getPublicUserByUsername() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(s3Service.generatePresignedUrl(user.getImage())).thenReturn("url");
            when(mapper.toPublicDTO(user, "url")).thenReturn(userDTO);

            assertThat(userService.getPublicUserByUsername(USERNAME)).isSameAs(userDTO);
            verify(mapper, never()).toDTO(any(User.class), anyString());
        }

        @Test
        @DisplayName("getPublicUserByUsername unknown user throws UserException")
        void getPublicUserByUsernameNotFound() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getPublicUserByUsername(USERNAME))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("full (self) projections")
    class FullProjections {

        @Test
        @DisplayName("getUserByUsername uses the full toDTO")
        void getUserByUsername() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(s3Service.generatePresignedUrl(user.getImage())).thenReturn("url");
            when(mapper.toDTO(user, "url")).thenReturn(userDTO);

            assertThat(userService.getUserByUsername(USERNAME)).isSameAs(userDTO);
        }

        @Test
        @DisplayName("getUserByUsername unknown user throws UserException")
        void getUserByUsernameNotFound() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByUsername(USERNAME)).isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("getUserWithImageUrl presigns the stored image")
        void getUserWithImageUrl() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(s3Service.generatePresignedUrl(user.getImage())).thenReturn("url");
            when(mapper.toDTO(user, "url")).thenReturn(userDTO);

            assertThat(userService.getUserWithImageUrl(USERNAME)).isSameAs(userDTO);
        }

        @Test
        @DisplayName("getUserWithImageUrl with no image passes a null url and skips S3")
        void getUserWithImageUrlNoImage() {
            user.setImage("");
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(mapper.toDTO(user, null)).thenReturn(userDTO);

            assertThat(userService.getUserWithImageUrl(USERNAME)).isSameAs(userDTO);
            verifyNoInteractions(s3Service);
        }
    }

    @Nested
    @DisplayName("searchAllByUsername")
    class Search {

        @Test
        @DisplayName("maps matches to SimpleUserDTO")
        void maps() {
            SimpleUserDTO simple = SimpleUserDTO.builder().username(USERNAME).build();
            when(repository.findAllByUsernameLike("mou")).thenReturn(List.of(user));
            when(s3Service.generatePresignedUrl(user.getImage())).thenReturn("url");
            when(mapper.toSimpleDTO(user, "url")).thenReturn(simple);

            assertThat(userService.searchAllByUsername("mou")).containsExactly(simple);
        }

        @Test
        @DisplayName("no matches returns an empty list")
        void empty() {
            when(repository.findAllByUsernameLike("zzz")).thenReturn(List.of());

            assertThat(userService.searchAllByUsername("zzz")).isEmpty();
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        private final SignupDTO request = SignupDTO.builder()
                .username(USERNAME).email(EMAIL).password("password1").build();

        @Test
        @DisplayName("saves the user with the 'User' role and the default image KEY")
        void success() {
            Role role = new Role();
            role.setName("User");
            when(repository.existsUsersByUsername(USERNAME)).thenReturn(false);
            when(repository.existsUsersByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.getRolesByName("User")).thenReturn(role);
            when(mapper.fromSignupDTO(request, role, DEFAULT_KEY, passwordEncoder)).thenReturn(user);
            when(repository.save(user)).thenReturn(user);
            when(s3Service.generatePresignedUrl(DEFAULT_KEY)).thenReturn("https://presigned");
            when(mapper.toDTO(user, "https://presigned")).thenReturn(userDTO);

            assertThat(userService.createUser(request)).isSameAs(userDTO);
            // the entity gets the key; only the returned DTO gets the presigned url
            verify(mapper).fromSignupDTO(eq(request), eq(role), eq(DEFAULT_KEY), eq(passwordEncoder));
            verify(mapper, never()).fromSignupDTO(any(), any(), eq("https://presigned"), any());
        }

        @Test
        @DisplayName("duplicate username throws UserException and saves nothing")
        void duplicateUsername() {
            when(repository.existsUsersByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("Username");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("duplicate email throws UserException and saves nothing")
        void duplicateEmail() {
            when(repository.existsUsersByUsername(USERNAME)).thenReturn(false);
            when(repository.existsUsersByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("Email");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCurrentUser")
    class DeleteCurrentUser {

        @Test
        @DisplayName("deletes the entity of the given username")
        void deletes() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            MessageResponse response = userService.deleteCurrentUser(USERNAME);

            assertThat(response.message()).isEqualTo("User deleted!");
            verify(repository).delete(user);
        }

        @Test
        @DisplayName("unknown username throws UserException and deletes nothing")
        void unknown() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteCurrentUser(USERNAME)).isInstanceOf(UserException.class);
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updateUserImage")
    class UpdateUserImage {

        private final MockMultipartFile file =
                new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        @Test
        @DisplayName("deletes the old key, uploads under users/<username>/ and saves the new key")
        void success() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            MessageResponse response = userService.updateUserImage(USERNAME, file);

            assertThat(response.message()).isEqualTo("Image updated successfully");
            verify(s3Service).deleteFile("users/mousty/old.png");
            ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
            verify(s3Service).uploadFile(eq(file), key.capture());
            assertThat(key.getValue()).startsWith("users/mousty/profile-").endsWith(".jpg");
            assertThat(user.getImage()).isEqualTo(key.getValue());
            verify(repository).saveAndFlush(user);
        }

        @Test
        @DisplayName("no previous image: nothing is deleted")
        void noPreviousImage() {
            user.setImage(null);
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            userService.updateUserImage(USERNAME, file);

            verify(s3Service, never()).deleteFile(any());
            verify(s3Service).uploadFile(eq(file), anyString());
        }

        @Test
        @DisplayName("failure deleting the old image does not block the upload")
        void deleteFailureIgnored() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            doThrow(new RuntimeException("s3 down")).when(s3Service).deleteFile(anyString());

            userService.updateUserImage(USERNAME, file);

            verify(s3Service).uploadFile(eq(file), anyString());
        }

        @Test
        @DisplayName("empty file throws IllegalArgumentException and touches no storage")
        void emptyFile() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            MockMultipartFile empty = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> userService.updateUserImage(USERNAME, empty))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(s3Service);
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("null file throws IllegalArgumentException")
        void nullFile() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updateUserImage(USERNAME, null))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(s3Service);
        }

        @Test
        @DisplayName("unknown user throws UserException")
        void unknownUser() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserImage(USERNAME, file))
                    .isInstanceOf(UserException.class);
            verifyNoInteractions(s3Service);
        }
    }

    @Nested
    @DisplayName("simple getters")
    class SimpleGetters {

        private final UUID unknownId = UUID.randomUUID();

        @Test
        @DisplayName("getUserEntityById returns the entity")
        void getUserEntityById() {
            when(repository.findById(user.getId())).thenReturn(Optional.of(user));

            assertThat(userService.getUserEntityById(user.getId())).isSameAs(user);
        }

        @Test
        @DisplayName("getUserEntityById unknown id throws UserException")
        void getUserEntityByIdNotFound() {
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserEntityById(unknownId)).isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("getUsernameById returns the username")
        void getUsernameById() {
            when(repository.findById(user.getId())).thenReturn(Optional.of(user));

            assertThat(userService.getUsernameById(user.getId())).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("getUsernameById unknown id throws UserException")
        void getUsernameByIdNotFound() {
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUsernameById(unknownId)).isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("getUserIdByUsername returns the id")
        void getUserIdByUsername() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThat(userService.getUserIdByUsername(USERNAME)).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("getUserIdByUsername unknown user throws UserException")
        void getUserIdByUsernameNotFound() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserIdByUsername(USERNAME)).isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("getUserEntityByUsername unknown user throws UserException")
        void getUserEntityByUsernameNotFound() {
            when(repository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserEntityByUsername(USERNAME))
                    .isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("generateImageUrl presigns the user's image key")
        void generateImageUrl() {
            when(s3Service.generatePresignedUrl(user.getImage())).thenReturn("url");

            assertThat(userService.generateImageUrl(user)).isEqualTo("url");
        }
    }
}
