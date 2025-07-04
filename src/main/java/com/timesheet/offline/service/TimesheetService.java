package com.timesheet.offline.service;

import com.timesheet.offline.model.ClockLog;
import com.timesheet.offline.repository.ClockLogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Service for handling timesheet-related operations like weekly resets and exports.
 */
@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final ClockLogRepository clockLogRepository;
    private final AuditService auditService;

    /**
     * Scheduled task to reset all timesheet data every Monday at midnight.
     */
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void weeklyReset() {
        clockLogRepository.deleteAllInBatch();
        auditService.log(null, "WEEKLY_RESET_SUCCESS", "SUCCESS", "Timesheet data cleared for the new week.");
        System.out.println("Weekly timesheet reset executed at " + LocalDateTime.now());
    }
    
    /**
     * Exports clock logs for a specific date range to a CSV format.
     * @param writer The writer to send the CSV data to.
     * @param startDate The start of the date range.
     * @param endDate The end of the date range.
     */
    public void exportWeeklyTimesheet(Writer writer, LocalDateTime startDate, LocalDateTime endDate) {
        List<ClockLog> logs = clockLogRepository.findByTimestampBetween(startDate, endDate);
        writeTimesheetToCsv(writer, logs);
    }

    /**
     * Exports all clock logs for the current week to a CSV format.
     * @param writer The writer to send the CSV data to.
     */
    public void exportWeeklyTimesheet(Writer writer) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7);
        List<ClockLog> logs = clockLogRepository.findByTimestampBetween(startOfWeek, endOfWeek);
        writeTimesheetToCsv(writer, logs);
    }

    private void writeTimesheetToCsv(Writer writer, List<ClockLog> logs) {
        String[] headers = {"LogID", "UserID", "UserEmail", "Action", "Timestamp (UTC)", "SessionID", "DurationHours"};
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (ClockLog log : logs) {
                // --- FIX APPLIED HERE ---
                // Format the timestamp in ISO 8601 format with a 'Z' to indicate UTC.
                // This ensures spreadsheet programs can interpret the timezone correctly.
                String formattedTimestamp = log.getTimestamp().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                
                csvPrinter.printRecord(
                        log.getId(),
                        log.getUser().getId(),
                        log.getUser().getEmail(),
                        log.getAction(),
                        formattedTimestamp,
                        log.getSessionId(),
                        log.getDurationHours() != null ? String.format("%.2f", log.getDurationHours()) : ""
                );
            }
            auditService.log(null, "TIMESHEET_EXPORT", "SUCCESS", "Timesheet exported.");
        } catch (IOException e) {
            auditService.log(null, "TIMESHEET_EXPORT_FAILURE", "FAILURE", "Error exporting timesheet: " + e.getMessage());
            throw new RuntimeException("Failed to write data to CSV file: " + e.getMessage());
        }
    }
}
