package com.timesheet.offline.service;

import com.timesheet.offline.dto.*;
import com.timesheet.offline.model.AuditLog;
import com.timesheet.offline.model.ClockAction;
import com.timesheet.offline.model.ClockLog;
import com.timesheet.offline.model.Role;
import com.timesheet.offline.model.User;
import com.timesheet.offline.repository.AuditLogRepository;
import com.timesheet.offline.repository.ClockLogRepository;
import com.timesheet.offline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for handling administrative tasks related to user management and exports.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final ClockLogRepository clockLogRepository;

    /**
     * --- FIX FOR 500 ERROR ---
     * This method now correctly uses the userMap it creates by passing it to the
     * toAuditLogDto helper method via a lambda function. This prevents the N+1 query problem.
     */
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        Page<AuditLog> auditLogPage = auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        
        Set<Long> userIds = auditLogPage.getContent().stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return auditLogPage.map(log -> toAuditLogDto(log, userMap));
    }

    public List<WeeklyTimesheetDto> getWeeklyTimesheet(LocalDate weekStartDate) {
        LocalDateTime startOfWeek = weekStartDate.atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7);

        List<ClockLog> logs = clockLogRepository.findByTimestampBetween(startOfWeek, endOfWeek);

        Map<User, List<ClockLog>> logsByUser = logs.stream()
                .collect(Collectors.groupingBy(ClockLog::getUser));

        List<WeeklyTimesheetDto> timesheets = new ArrayList<>();
        for (Map.Entry<User, List<ClockLog>> entry : logsByUser.entrySet()) {
            User user = entry.getKey();
            List<ClockLog> userLogs = entry.getValue();

            Map<LocalDate, Double> dailyHours = userLogs.stream()
                    .filter(log -> log.getAction() == ClockAction.CLOCK_OUT && log.getDurationHours() != null)
                    .collect(Collectors.groupingBy(
                            log -> log.getTimestamp().toLocalDate(),
                            Collectors.summingDouble(ClockLog::getDurationHours)
                    ));

            Map<String, Double> formattedDailyHours = new HashMap<>();
            for(int i=0; i<7; i++){
                LocalDate day = weekStartDate.plusDays(i);
                formattedDailyHours.put(day.toString(), dailyHours.getOrDefault(day, 0.0));
            }

            double totalHours = formattedDailyHours.values().stream().mapToDouble(Double::doubleValue).sum();

            timesheets.add(WeeklyTimesheetDto.builder()
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .dailyHours(formattedDailyHours)
                    .totalHours(totalHours)
                    .build());
        }
        return timesheets;
    }

    /**
     * --- FIX FOR 500 ERROR ---
     * This method now correctly uses the userMap it creates by passing it to the
     * formatLogAsNotification helper method via a lambda function.
     */
    public List<NotificationDto> getNotifications() {
        Pageable limit = PageRequest.of(0, 20);
        Page<AuditLog> recentLogsPage = auditLogRepository.findAllByOrderByTimestampDesc(limit);
        List<AuditLog> recentLogs = recentLogsPage.getContent();

        Set<Long> userIds = recentLogs.stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return recentLogs.stream()
                .map(log -> formatLogAsNotification(log, userMap))
                .collect(Collectors.toList());
    }

    public List<UserStatusDto> getUserStatuses() {
        List<User> users = userRepository.findAll();
        return users.stream().map(user -> {
            Optional<ClockLog> lastLogOpt = clockLogRepository.findTopByUserIdOrderByTimestampDesc(user.getId());
            String status = "Never Clocked In";
            java.time.LocalDateTime lastActionTimestamp = null;

            if (lastLogOpt.isPresent()) {
                ClockLog lastLog = lastLogOpt.get();
                status = lastLog.getAction() == ClockAction.CLOCK_IN ? "Clocked In" : "Clocked Out";
                lastActionTimestamp = lastLog.getTimestamp();
            }

            return UserStatusDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .status(status)
                    .lastActionTimestamp(lastActionTimestamp)
                    .build();
        }).collect(Collectors.toList());
    }

    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public UserDto createUser(CreateUserRequestDto createUserRequestDto) {
        if (userRepository.existsByEmail(createUserRequestDto.getEmail())) {
            throw new IllegalArgumentException("Email is already in use.");
        }
        
        if (createUserRequestDto.getRole() == Role.ROLE_EMPLOYEE) {
            List<User> employees = userRepository.findAllByRole(Role.ROLE_EMPLOYEE);
            boolean pinExists = employees.stream()
                    .anyMatch(employee -> passwordEncoder.matches(createUserRequestDto.getPin(), employee.getPassword()));
            if (pinExists) {
                throw new IllegalArgumentException("This PIN is already in use by another employee. Please choose a unique PIN.");
            }
        }

        User user = new User();
        user.setEmail(createUserRequestDto.getEmail());
        user.setRole(createUserRequestDto.getRole());
        user.setPassword(passwordEncoder.encode(createUserRequestDto.getPin()));

        User savedUser = userRepository.save(user);
        auditService.log(null, "USER_CREATE_SUCCESS", "SUCCESS", "Admin created user: " + savedUser.getEmail());
        return toUserDto(savedUser);
    }
    
    @Transactional
    public void createUser(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for deletion."));
        userRepository.deleteById(userId);
        auditService.log(null, "USER_DELETE_SUCCESS", "SUCCESS", "Admin deleted user: " + user.getEmail());
    }

    @Transactional
    public void resetUserPin(Long userId, String newPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for PIN reset."));
        
        if (user.getRole() == Role.ROLE_EMPLOYEE) {
             List<User> otherEmployees = userRepository.findAllByRoleAndIdNot(Role.ROLE_EMPLOYEE, userId);
             boolean pinExists = otherEmployees.stream()
                    .anyMatch(employee -> passwordEncoder.matches(newPin, employee.getPassword()));
            if (pinExists) {
                throw new IllegalArgumentException("This PIN is already in use by another employee. Please choose a unique PIN.");
            }
        }
        
        user.setPassword(passwordEncoder.encode(newPin));
        userRepository.save(user);
        auditService.log(null, "USER_CREDENTIALS_RESET_SUCCESS", "SUCCESS", "Admin reset credentials for user: " + user.getEmail());
    }

    public void exportAuditLogs(Writer writer, LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> logs = auditLogRepository.findByTimestampBetween(startDate, endDate);
        writeAuditLogsToCsv(writer, logs);
    }

    public void exportAuditLogs(Writer writer) {
        List<AuditLog> logs = auditLogRepository.findAll();
        writeAuditLogsToCsv(writer, logs);
    }

    private void writeAuditLogsToCsv(Writer writer, List<AuditLog> logs) {
        Set<Long> userIds = logs.stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        String[] headers = {"LogID", "Timestamp (UTC)", "UserEmail", "Action", "Status", "IP Address", "Details"};
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (AuditLog log : logs) {
                String formattedTimestamp = log.getTimestamp() != null ? log.getTimestamp().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "N/A";
                String userEmail = "System";
                if (log.getUserId() != null) {
                    User user = userMap.get(log.getUserId());
                    userEmail = (user != null) ? user.getEmail() : "Unknown User (ID: " + log.getUserId() + ")";
                }
                
                csvPrinter.printRecord(
                        log.getId(),
                        formattedTimestamp,
                        userEmail,
                        log.getAction(),
                        log.getStatus(),
                        log.getIpAddress(),
                        log.getDetails()
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write audit logs to CSV file: " + e.getMessage());
        }
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
    
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }
        String namePart = email.split("@")[0];
        return Stream.of(namePart.split("[._-]"))
                .filter(part -> !part.isEmpty())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
    
    private NotificationDto formatLogAsNotification(AuditLog log, Map<Long, User> userMap) {
        String message = "An unspecified action occurred.";
        if (log.getDetails() != null && !log.getDetails().isBlank()) {
            message = log.getDetails();
        }

        if (log.getAction() != null) {
            String userName = "System";
            if (log.getUserId() != null) {
                User user = userMap.get(log.getUserId());
                userName = (user != null) ? extractNameFromEmail(user.getEmail()) : "An unknown user";
            }

            switch (log.getAction()) {
                case "CLOCK_IN_SUCCESS":
                    message = userName + " clocked in.";
                    break;
                case "CLOCK_OUT_SUCCESS":
                    message = userName + " clocked out.";
                    break;
                case "ADMIN_LOGIN_SUCCESS":
                    message = userName + " logged into the admin dashboard.";
                    break;
            }
        }
        
        return NotificationDto.builder()
                .id(log.getId())
                .message(message)
                .timestamp(log.getTimestamp())
                .build();
    }
    
    private AuditLogDto toAuditLogDto(AuditLog log, Map<Long, User> userMap) {
        String userEmail = "System";
        if (log.getUserId() != null) {
            User user = userMap.get(log.getUserId());
            userEmail = (user != null) ? user.getEmail() : "Unknown User";
        }
        return AuditLogDto.builder()
                .id(log.getId())
                .timestamp(log.getTimestamp())
                .action(log.getAction())
                .status(log.getStatus())
                .userEmail(userEmail)
                .details(log.getDetails())
                .build();
    }
}
