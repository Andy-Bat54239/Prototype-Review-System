package com.auca.prbs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "supervisor_id", nullable = false)
    private Long supervisorId;

    /** Snapshot of the student's display name at booking time (denormalized for email). */
    @Column(nullable = false)
    private String name;

    /** Project group number from the original prototype's data model. */
    @Column(name = "group_number", nullable = false)
    private int groupNumber;

    @Column(nullable = false, length = 200)
    private String project;

    /** Slot start time. Combining date + time into one column makes the reminder query trivial. */
    @Column(name = "slot_at", nullable = false)
    private LocalDateTime slotAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BookingStatus status;

    @Column(name = "meet_url", nullable = false, length = 500)
    private String meetUrl;

    /** Set true after ReminderScheduler emails the upcoming-session reminder; prevents duplicates. */
    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
