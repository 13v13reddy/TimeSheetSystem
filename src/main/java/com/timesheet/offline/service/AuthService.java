package com.timesheet.offline.service;

import com.timesheet.offline.dto.*;
import com.timesheet.offline.model.ClockAction;
import com.timesheet.offline.model.ClockLog;
import com.timesheet.offline.model.Role;
import com.timesheet.offline.model.User;
import com.timesheet.offline.repository.ClockLogRepository;
import com.timesheet.offline.repository.UserRepository;
import com.timesheet.offline.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for handling all authentication logic,
 * including employee clock-in/out and admin login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClockLogRepository clockLogRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Handles the clock-in and clock-out logic for employees using only a unique PIN.
     * This method iterates through employees to find a match for the provided PIN.
     * @param pin The unique PIN submitted by the employee.
     * @return A DTO with the result of the clock action.
     * @throws AuthenticationException if no user with a matching PIN is found.
     */
    @Transactional
    public ClockResponseDto handlePinClockAction(String pin) {
        List<User> employees = userRepository.findAllByRole(Role.ROLE_EMPLOYEE);

        User matchedEmployee = employees.stream()
                .filter(employee -> passwordEncoder.matches(pin, employee.getPassword()))
                .findFirst()
                .orElseThrow(() -> {
                    auditService.log(null, "PIN_LOGIN_FAILURE", "FAILURE", "Failed PIN login attempt. No matching user found.");
                    return new AuthenticationException("Invalid PIN provided.") {};
                });

        Optional<ClockLog> lastLogOpt = clockLogRepository.findTopByUserIdOrderByTimestampDesc(matchedEmployee.getId());

        ClockLog newLog = new ClockLog();
        newLog.setUser(matchedEmployee);
        newLog.setTimestamp(LocalDateTime.now());
        String message;
        double hoursWorked = 0.0;
        
        // --- NAME PARSING LOGIC ADDED ---
        String employeeName = extractNameFromEmail(matchedEmployee.getEmail());

        if (lastLogOpt.isEmpty() || lastLogOpt.get().getAction() == ClockAction.CLOCK_OUT) {
            newLog.setAction(ClockAction.CLOCK_IN);
            newLog.setSessionId(UUID.randomUUID().toString());
            message = "Welcome, " + employeeName + "! Clock-in successful.";
            auditService.log(matchedEmployee.getId(), "CLOCK_IN_SUCCESS", "SUCCESS", "User clocked in via PIN-only kiosk.");
        } else {
            ClockLog lastClockIn = lastLogOpt.get();
            newLog.setAction(ClockAction.CLOCK_OUT);
            newLog.setSessionId(lastClockIn.getSessionId());
            Duration duration = Duration.between(lastClockIn.getTimestamp(), newLog.getTimestamp());
            
            double totalMinutes = duration.toMinutes();
            hoursWorked = totalMinutes / 60.0;
            
            newLog.setDurationHours(hoursWorked);
            message = "Goodbye, " + employeeName + "! Clock-out successful.";
            auditService.log(matchedEmployee.getId(), "CLOCK_OUT_SUCCESS", "SUCCESS", "User clocked out. Hours worked: " + String.format("%.2f", hoursWorked));
        }

        clockLogRepository.save(newLog);

        return ClockResponseDto.builder()
                .message(message)
                .userEmail(matchedEmployee.getEmail())
                .action(newLog.getAction().name())
                .timestamp(newLog.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .hoursWorkedThisSession(hoursWorked)
                .build();
    }

    public JwtResponseDto handleAdminLogin(AdminLoginRequestDto request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            auditService.log(null, "ADMIN_LOGIN_FAILURE", "FAILURE", "Failed admin login attempt for email: " + request.getEmail());
            throw e;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
             .orElseThrow(() -> new RuntimeException("User not found after successful authentication. This should not happen."));

        if (user.getRole() != Role.ROLE_ADMIN) {
            auditService.log(user.getId(), "ADMIN_LOGIN_FAILURE", "FAILURE", "Non-admin user attempted to log into admin portal.");
            throw new IllegalStateException("Access denied. Not an administrator.");
        }
        
        String jwtToken = jwtService.generateToken(userDetails);
        auditService.log(user.getId(), "ADMIN_LOGIN_SUCCESS", "SUCCESS", "Admin successfully logged in.");

        return JwtResponseDto.builder()
            .token(jwtToken)
            .email(user.getEmail())
            .role(user.getRole().name())
            .build();
    }
    
    /**
     * --- NEW HELPER METHOD ---
     * A utility method to extract and format a name from an email address.
     * It handles formats like "john.doe@..." and "johndoe@..."
     * @param email The user's email address.
     * @return A formatted, capitalized name.
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User"; // Fallback for invalid email formats
        }
        String namePart = email.split("@")[0];
        
        // Replace common separators with a space and capitalize each word
        return Stream.of(namePart.split("[._-]"))
                .filter(part -> !part.isEmpty())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
