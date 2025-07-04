package com.timesheet.offline.dto;

import com.timesheet.offline.model.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for representing a user's status, including their last clock action.
 */
@Data
@Builder
public class UserStatusDto {
    private Long id;
    private String email;
    private Role role;
    private String status; // e.g., "Clocked In", "Clocked Out"
    private LocalDateTime lastActionTimestamp;
}
