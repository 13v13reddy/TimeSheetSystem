package com.timesheet.offline.repository;

import com.timesheet.offline.model.ClockAction;
import com.timesheet.offline.model.ClockLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the ClockLog entity.
 */
@Repository
public interface ClockLogRepository extends JpaRepository<ClockLog, Long> {

    /**
     * Finds the most recent clock log for a given user ID.
     * This is used to determine if a user is currently clocked in or out.
     * @param userId The ID of the user.
     * @return An Optional containing the latest ClockLog if one exists.
     */
    Optional<ClockLog> findTopByUserIdOrderByTimestampDesc(Long userId);

    /**
     * Finds all clock logs within a given date range.
     * Used for exporting weekly timesheets.
     * @param start The start of the date range.
     * @param end The end of the date range.
     * @return A list of clock logs.
     */
    List<ClockLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Deletes all clock logs in a single batch operation.
     * This is used for the scheduled weekly reset.
     */
    @Modifying
    @Query("DELETE FROM ClockLog")
    void deleteAllInBatch();
}
