package com.auca.prbs.user.entity;

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

/** Singleton row id=1. Owned by user-service; booking-service reads via Feign. */
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

    @Column(name = "otp_expiry",    nullable = false) private int otpExpiry;
    @Column(name = "cancel_window", nullable = false) private int cancelWindow;
    @Column(name = "reminder_time", nullable = false) private int reminderTime;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
