package com.timesheet.offline.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for resetting a user's PIN or password.
 */
@Data
public class ResetPinRequestDto {
    @NotBlank
    private String newPin;
}
