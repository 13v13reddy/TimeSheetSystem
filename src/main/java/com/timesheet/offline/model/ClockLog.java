package com.timesheet.offline.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single clock-in or clock-out event.
 * Maps to the 'clock_logs' table.
 */
@Entity
@Table(name = "clock_logs")
@Data
@NoArgsConstructor
public class ClockLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClockAction action; // CLOCK_IN or CLOCK_OUT

    @NotNull
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private String sessionId; // Groups a CLOCK_IN and CLOCK_OUT pair

    @Column
    private Double durationHours; // Calculated and stored on CLOCK_OUT
}
