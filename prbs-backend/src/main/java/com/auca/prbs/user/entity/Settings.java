package com.auca.prbs.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "settings")
public class Settings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "otp_expiry",    nullable = false) private int otpExpiry;
    @Column(name = "cancel_window", nullable = false) private int cancelWindow;
    @Column(name = "reminder_time", nullable = false) private int reminderTime;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Settings() {}

    public Settings(Long id, int otpExpiry, int cancelWindow, int reminderTime, Instant updatedAt) {
        this.id = id;
        this.otpExpiry = otpExpiry;
        this.cancelWindow = cancelWindow;
        this.reminderTime = reminderTime;
        this.updatedAt = updatedAt;
    }

    public Long    getId()            { return id; }
    public int     getOtpExpiry()     { return otpExpiry; }
    public int     getCancelWindow()  { return cancelWindow; }
    public int     getReminderTime()  { return reminderTime; }
    public Instant getUpdatedAt()     { return updatedAt; }

    public void setId(Long id)                       { this.id = id; }
    public void setOtpExpiry(int otpExpiry)          { this.otpExpiry = otpExpiry; }
    public void setCancelWindow(int cancelWindow)    { this.cancelWindow = cancelWindow; }
    public void setReminderTime(int reminderTime)    { this.reminderTime = reminderTime; }
    public void setUpdatedAt(Instant updatedAt)      { this.updatedAt = updatedAt; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
