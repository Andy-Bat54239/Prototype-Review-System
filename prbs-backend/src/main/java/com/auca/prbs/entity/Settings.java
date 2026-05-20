package com.auca.prbs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Singleton system settings — row id=1 is the only one expected to exist.
 * Seeded by Flyway migration V3.
 */
@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /** OTP code lifetime in minutes (default 10). */
    @Column(name = "otp_expiry", nullable = false)
    private int otpExpiry;

    /** Minutes before the slot when a booking can no longer be cancelled (default 60). */
    @Column(name = "cancel_window", nullable = false)
    private int cancelWindow;

    /** Minutes before the slot when the reminder email is sent (default 30). */
    @Column(name = "reminder_time", nullable = false)
    private int reminderTime;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
