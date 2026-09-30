package com.mousty.gymbro.mapper;

import com.mousty.gymbro.entity.Role;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import org.mapstruct.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        builder = @Builder(disableBuilder = true)
)
public interface UserMapper extends UserBaseMapper {

    /** Full profile — only for the account owner (login, /auth/profile). */
    @Mapping(target = "roleName", source = "user.role.name")
    @Mapping(target = "image", source = "image")
    UserDTO toDTO(User user, String image);

    /** Another user's profile: no email, last name, birth date or role. */
    @Mapping(target = "image", source = "image")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "roleName", ignore = true)
    UserDTO toPublicDTO(User user, String image);

    // Used by every mapper that embeds a user (posts, comments, likes, workouts, exercises),
    // so it gets the public projection too.
    @Override
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "roleName", ignore = true)
    UserDTO toDTO(User user);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "verifyOtp", ignore = true)
    @Mapping(target = "verifyOtpExpiredAt", ignore = true)
    @Mapping(target = "resetOtp", ignore = true)
    @Mapping(target = "resetOtpExpiredAt", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "workoutHistories", ignore = true)
    User toEntity(UserDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", expression = "java(passwordEncoder.encode(signupDTO.password()))")
    @Mapping(target = "isAccountVerified", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "role", source = "role")
    @Mapping(target = "image", source = "image")
    @Mapping(target = "verifyOtp", ignore = true)
    @Mapping(target = "verifyOtpExpiredAt", ignore = true)
    @Mapping(target = "resetOtp", ignore = true)
    @Mapping(target = "resetOtpExpiredAt", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "workoutHistories", ignore = true)
    User fromSignupDTO(SignupDTO signupDTO, Role role, String image, @Context PasswordEncoder passwordEncoder);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "isAccountVerified", source = "user.isAccountVerified")
    @Mapping(target = "image", source = "image")
    SimpleUserDTO toSimpleDTO(User user, String image);


}
