package com.mousty.gymbro.dto.user;

import com.mousty.gymbro.validation.Put;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInput {
    @NotNull(message = "id is required", groups = {Put.class, Default.class})
    private String id;
    @NotBlank(message = "Username is required")
    private String username;
    @NotBlank(message = "email is required")
    private String email;
    @NotBlank(message = "firstName is required")
    private String firstName;
    @NotBlank(message = "lastName is required")
    private String lastName;
    @NotNull(message = "Birth date is required")
    private LocalDate birthDate;
}
