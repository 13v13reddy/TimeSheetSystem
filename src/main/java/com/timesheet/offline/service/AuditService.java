package com.timesheet.offline.service;

import com.timesheet.offline.model.AuditLog;
import com.timesheet.offline.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for creating audit log entries.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Creates and saves a new audit log.
     * @param userId The ID of the user performing the action (can be null for system events).
     * @param action A description of the action performed.
     * @param status The status of the action (e.g., SUCCESS, FAILURE).
     * @param details Additional details about the event.
     */
    public void log(Long userId, String action, String status, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setStatus(status);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        // In a real web context, you'd get the IP from the HttpServletRequest.
        // For this offline system, we'll leave it null for now.
        log.setIpAddress(null);
        auditLogRepository.save(log);
    }
}
