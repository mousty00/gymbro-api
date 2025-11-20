package com.mousty.gymbro.response;

import com.mousty.gymbro.dto.user.UserDTO;
import lombok.Builder;

@Builder
public record LoginResponse (
        String message,
        UserDTO result,
        String token
){
}
