package com.mousty.gymbro.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleUserDTO {
    @NotNull(message = "id is required")
    private UUID id;
    @NotBlank(message = "Username is required")
    private String username;
    private String image;
    private Boolean isAccountVerified;
}
