package com.mousty.gymbro.dto.friendship;

import com.mousty.gymbro.dto.user.SimpleUserDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendshipDTO {

    @NotNull
    private UUID id;

    @NotNull
    private SimpleUserDTO user;

    @NotNull
    private SimpleUserDTO friend;

    @Size(max = 20)
    @NotNull
    private String status;

    @NotNull
    private Instant createdAt;

}
