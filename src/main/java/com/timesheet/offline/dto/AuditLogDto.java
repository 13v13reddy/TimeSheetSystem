package com.timesheet.offline.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO for representing a single audit log entry for the admin dashboard.
 */
@Data
@Builder
public class AuditLogDto {
    private Long id;
    private LocalDateTime timestamp;
    private String action;
    private String status;
    private String userEmail; // The email of the user who performed the action
    private String details;
}
