package com.mousty.gymbro.controller.graphql;

import com.mousty.gymbro.generic.PageableDefaults;
import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.dto.user.SimpleUserDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.response.MessageResponse;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@DgsComponent
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @DgsQuery
    public Connection<UserDTO> users(
            @InputArgument @Nullable Integer page,
            @InputArgument @Nullable Integer size
    ) {
        Pageable pageable = PageableDefaults.INSTANCE.create(page, size);
        return service.getAllUsers(pageable);
    }

    @DgsQuery
    public UserDTO user(
            @InputArgument @Nullable UUID id
    ) {
        return service.getUserById(id);
    }

    @DgsQuery
    public List<SimpleUserDTO> searchUser(@InputArgument String username) {
        return service.searchAllByUsername(username);
    }

    @DgsMutation
    public MessageResponse deleteUser(@InputArgument UUID id) {
        return service.deleteUserById(id);
    }

}