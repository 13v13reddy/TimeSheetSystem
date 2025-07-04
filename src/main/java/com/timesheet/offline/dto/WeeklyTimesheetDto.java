package com.timesheet.offline.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * DTO for representing a user's weekly timesheet summary.
 */
@Data
@Builder
public class WeeklyTimesheetDto {
    private Long userId;
    private String userEmail;
    private Map<String, Double> dailyHours; // Key: "YYYY-MM-DD", Value: Hours
    private Double totalHours;
}
