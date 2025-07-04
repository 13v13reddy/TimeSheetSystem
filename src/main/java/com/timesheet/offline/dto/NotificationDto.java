package com.timesheet.offline.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for representing a single notification item for the admin dashboard.
 */
@Data
@Builder
public class NotificationDto {
    private Long id;
    private String message;
    private LocalDateTime timestamp;
}
