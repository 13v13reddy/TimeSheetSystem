package com.timesheet.offline.dto;

import com.timesheet.offline.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a new user.
 * The 'pin' field is used for both employee PINs and admin passwords upon creation.
 */
@Data
public class CreateUserRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String pin;

    @NotNull
    private Role role;
}
