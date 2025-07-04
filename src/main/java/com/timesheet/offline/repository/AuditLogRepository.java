package com.timesheet.offline.repository;

import com.timesheet.offline.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for the AuditLog entity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Finds the most recent audit logs, ordered by timestamp descending.
     * The Pageable parameter is used to limit the number of results (e.g., get the top 20).
     * @param pageable The paging information.
     * @return A list of the most recent audit logs.
     */
    List<AuditLog> findByOrderByTimestampDesc(Pageable pageable);

    /**
     * Finds all audit logs within a given date range.
     * This is used for exporting logs for a specific period.
     * @param startDate The start of the date range.
     * @param endDate The end of the date range.
     * @return A list of audit logs within the specified range.
     */
    List<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * --- NEW METHOD ---
     * Finds all audit logs, ordered by timestamp descending, with pagination support.
     * This is used for the paginated audit log view in the admin dashboard.
     * @param pageable The pagination information.
     * @return A paginated list of audit logs.
     */
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
