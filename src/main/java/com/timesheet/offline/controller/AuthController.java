package com.timesheet.offline.controller;

import com.timesheet.offline.dto.AdminLoginRequestDto;
import com.timesheet.offline.dto.ClockResponseDto;
import com.timesheet.offline.dto.JwtResponseDto;
import com.timesheet.offline.dto.PinLoginRequestDto;
import com.timesheet.offline.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling all authentication requests.
 * Endpoints in this controller are publicly accessible as defined in SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * --- UPDATED ENDPOINT ---
     * Endpoint for employees to clock in or out using only their unique PIN.
     * @param loginRequest DTO containing the PIN.
     * @return A response entity with a confirmation message.
     */
    @PostMapping("/kiosk/login")
    public ResponseEntity<ClockResponseDto> clockInOrOutWithPin(@Valid @RequestBody PinLoginRequestDto loginRequest) {
        ClockResponseDto response = authService.handlePinClockAction(loginRequest.getPin());
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint for administrators to log in using their email and password.
     * @param loginRequest DTO containing email and password.
     * @return A response entity with a JWT for session management.
     */
    @PostMapping("/admin/login")
    public ResponseEntity<JwtResponseDto> adminLogin(@Valid @RequestBody AdminLoginRequestDto loginRequest) {
        JwtResponseDto response = authService.handleAdminLogin(loginRequest);
        return ResponseEntity.ok(response);
    }
}
