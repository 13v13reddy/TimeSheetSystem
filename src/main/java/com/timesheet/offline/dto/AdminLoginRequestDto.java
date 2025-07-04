package com.timesheet.offline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for handling admin login requests via password.
 */
@Data
public class AdminLoginRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
