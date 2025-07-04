package com.timesheet.offline;

import com.timesheet.offline.model.Role;
import com.timesheet.offline.model.User;
import com.timesheet.offline.service.AdminService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Spring Boot application.
 * @EnableScheduling is included to support the weekly timesheet reset task.
 */
@SpringBootApplication
@EnableScheduling
public class TimesheetSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimesheetSystemApplication.class, args);
    }

    /**
     * This runner executes on startup and creates a default admin user
     * if one doesn't already exist. This is crucial for initial setup.
     * The PIN is now treated as a password for the admin user.
     * @param adminService The service to manage users.
     * @return A CommandLineRunner bean.
     */
    @Bean
    CommandLineRunner run(AdminService adminService) {
        return args -> {
            if (!adminService.userExists("admin@system.local")) {
                 User admin = new User();
                admin.setEmail("admin@system.local");
                admin.setRole(Role.ROLE_ADMIN);
                adminService.createUser(admin, "admin123"); // "admin123" is the password
                System.out.println("Default admin user created. Email: admin@system.local, Password: admin123");
            }
        };
    }
}
