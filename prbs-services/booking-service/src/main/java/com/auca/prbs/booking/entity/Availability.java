package com.auca.prbs.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supervisor_id", nullable = false) private Long supervisorId;
    @Column(nullable = false)                          private LocalDate date;
    @Column(name = "start_time", nullable = false)     private LocalTime startTime;
    @Column(name = "end_time",   nullable = false)     private LocalTime endTime;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(name = "meet_url", nullable = false, length = 500) private String meetUrl;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
