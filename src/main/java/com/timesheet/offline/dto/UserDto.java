package com.timesheet.offline.dto;

import com.timesheet.offline.model.Role;
import lombok.Builder;
import lombok.Data;

/**
 * A public-facing DTO representing a User, excluding sensitive data like the password.
 */
@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private Role role;
}
