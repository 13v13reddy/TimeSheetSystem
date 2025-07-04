package com.timesheet.offline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for returning a JWT upon successful login.
 */
@Data
@Builder
@AllArgsConstructor
public class JwtResponseDto {
    private String token;
    private String email;
    private String role;
}
