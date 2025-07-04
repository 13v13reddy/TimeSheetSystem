package com.timesheet.offline.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents an audit trail entry for significant system events.
 * Maps to the 'audit_logs' table.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // Can be null for system events

    @NotBlank
    @Column(nullable = false)
    private String action; // e.g., "USER_LOGIN_SUCCESS", "USER_CREATE_FAILURE"

    @NotBlank
    @Column(nullable = false)
    private String status; // SUCCESS or FAILURE

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT") // For potentially long details
    private String details;
}
