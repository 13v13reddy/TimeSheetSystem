package com.timesheet.offline.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for handling employee clock-in/out requests via PIN.
 * This DTO is now used for the PIN-only kiosk.
 */
@Data
public class PinLoginRequestDto {
    @NotBlank
    private String pin;
}
