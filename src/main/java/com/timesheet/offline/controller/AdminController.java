package com.timesheet.offline.controller;

import com.timesheet.offline.dto.*;
import com.timesheet.offline.service.AdminService;
import com.timesheet.offline.service.TimesheetService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for handling all administrative actions.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final TimesheetService timesheetService;

    /**
     * --- NEW ENDPOINT ---
     * Endpoint to retrieve a paginated list of audit logs.
     * @param pageable The pagination information (e.g., ?page=0&size=20).
     * @return A paginated list of audit logs.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAuditLogs(pageable));
    }

    @GetMapping("/timesheets")
    public ResponseEntity<List<WeeklyTimesheetDto>> getWeeklyTimesheet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return ResponseEntity.ok(adminService.getWeeklyTimesheet(weekStartDate));
    }

    @GetMapping("/users/statuses")
    public ResponseEntity<List<UserStatusDto>> getUserStatuses() {
        return ResponseEntity.ok(adminService.getUserStatuses());
    }
    
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        return ResponseEntity.ok(adminService.getNotifications());
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        return new ResponseEntity<>(adminService.createUser(request), HttpStatus.CREATED);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/reset-pin")
    public ResponseEntity<Void> resetPin(@PathVariable Long id, @Valid @RequestBody ResetPinRequestDto request) {
        adminService.resetUserPin(id, request.getNewPin());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/timesheets/export")
    public void exportTimesheets(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletResponse response) throws IOException {
        
        String fileName = "timesheet_export_" + java.time.LocalDate.now() + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        if (startDate != null && endDate != null) {
            timesheetService.exportWeeklyTimesheet(response.getWriter(), startDate, endDate);
        } else {
            timesheetService.exportWeeklyTimesheet(response.getWriter());
        }
    }

    @GetMapping("/audit-logs/export")
    public void exportAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletResponse response) throws IOException {
        
        String fileName = "audit_logs_export_" + java.time.LocalDate.now() + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        if (startDate != null && endDate != null) {
            adminService.exportAuditLogs(response.getWriter(), startDate, endDate);
        } else {
            adminService.exportAuditLogs(response.getWriter());
        }
    }
}
