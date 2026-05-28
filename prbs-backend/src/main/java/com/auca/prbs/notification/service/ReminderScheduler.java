package com.auca.prbs.notification.service;

import com.auca.prbs.booking.entity.Booking;
import com.auca.prbs.booking.entity.BookingStatus;
import com.auca.prbs.booking.repository.BookingRepository;
import com.auca.prbs.user.entity.Settings;
import com.auca.prbs.user.entity.User;
import com.auca.prbs.user.repository.SettingsRepository;
import com.auca.prbs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final int DEFAULT_REMINDER_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;
    private final EmailService emailService;

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
        User student = userRepository.findById(b.getStudentId()).orElse(null);
        if (student == null) {
            log.warn("Cannot send reminder for booking {} — student lookup failed", b.getId());
            return;
        }
        try {
            emailService.sendReminder(
                    student.getEmail(),
                    student.getName(),
                    b.getProject(),
                    DATE_FMT.format(b.getSlotAt()),
                    TIME_FMT.format(b.getSlotAt()),
                    b.getMeetUrl()
            );
            b.setReminderSent(true);
            bookingRepository.save(b);
        } catch (Exception e) {
            log.warn("Reminder dispatch failed for booking {}: {}", b.getId(), e.getMessage());
        }
    }

    private int safeReminderWindow() {
        try {
            return settingsRepository.findById(Settings.SINGLETON_ID)
                    .map(Settings::getReminderTime)
                    .orElse(DEFAULT_REMINDER_MINUTES);
        } catch (Exception e) {
            return DEFAULT_REMINDER_MINUTES;
        }
    }
}
