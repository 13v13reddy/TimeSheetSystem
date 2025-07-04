package com.timesheet.offline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for responding to a successful clock-in/out action.
 */
@Data
@Builder
@AllArgsConstructor
public class ClockResponseDto {
    private String message;
    private String userEmail;
    private String action;
    private String timestamp;
    private Double hoursWorkedThisSession;
}
