package com.mousty.gymbro.mapper;

import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.dto.user.UserDTO;

public interface UserBaseMapper extends GenericMapper<User, UserDTO> {

    UserDTO toDTO(User user, String imageUrl);
    User toEntity(UserDTO dto);
    //User toNewEntity(UserDTO dto);
    User fromSignupDTO(SignupDTO dto);
}
