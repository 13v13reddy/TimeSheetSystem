package com.timesheet.offline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for handling clock-in/out requests.
 */
@Data
public class LoginRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String pin;
}