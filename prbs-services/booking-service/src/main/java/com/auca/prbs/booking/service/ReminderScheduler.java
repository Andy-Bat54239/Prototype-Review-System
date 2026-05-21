package com.auca.prbs.booking.service;

import com.auca.prbs.booking.client.NotificationClient;
import com.auca.prbs.booking.client.SettingsClient;
import com.auca.prbs.booking.client.UserServiceClient;
import com.auca.prbs.booking.client.UserView;
import com.auca.prbs.booking.entity.Booking;
import com.auca.prbs.booking.entity.BookingStatus;
import com.auca.prbs.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lives in booking-service (which owns the booking rows). Calls
 * notification-service to actually send the reminder. Idempotent via
 * {@code reminder_sent}.
 */
@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final int DEFAULT_REMINDER_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final UserServiceClient userServiceClient;
    private final SettingsClient settingsClient;
    private final NotificationClient notificationClient;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendDueReminders() {
        int windowMinutes = safeReminderWindow();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusMinutes(windowMinutes);

        List<Booking> due = bookingRepository.findByStatusAndReminderSentFalseAndSlotAtBetween(
                BookingStatus.CONFIRMED, now, end);

        for (Booking b : due) sendOne(b);
        if (!due.isEmpty()) log.info("Reminder scheduler: sent {} reminder(s)", due.size());
    }

    private void sendOne(Booking b) {
        UserView student = userServiceClient.findById(b.getStudentId()).orElse(null);
        if (student == null) {
            log.warn("Cannot send reminder for booking {} — student lookup failed", b.getId());
            return;
        }
        Map<String, String> payload = new HashMap<>();
        payload.put("studentEmail", student.email());
        payload.put("studentName",  student.name());
        payload.put("project",      b.getProject());
        payload.put("date",         DATE_FMT.format(b.getSlotAt()));
        payload.put("time",         TIME_FMT.format(b.getSlotAt()));
        payload.put("meetUrl",      b.getMeetUrl());

        try {
            notificationClient.sendReminder(payload);
            b.setReminderSent(true);
            bookingRepository.save(b);
        } catch (Exception e) {
            log.warn("Reminder dispatch failed for booking {}: {}", b.getId(), e.getMessage());
        }
    }

    private int safeReminderWindow() {
        try {
            return settingsClient.get().reminderTime();
        } catch (Exception e) {
            return DEFAULT_REMINDER_MINUTES;
        }
    }
}
