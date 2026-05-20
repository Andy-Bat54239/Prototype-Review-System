package com.auca.prbs.service;

import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import com.auca.prbs.entity.Settings;
import com.auca.prbs.entity.User;
import com.auca.prbs.repository.BookingRepository;
import com.auca.prbs.repository.SettingsRepository;
import com.auca.prbs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Phase 7: fires once a minute, finds confirmed bookings whose slot is within
 * the {@code settings.reminderTime} window and that haven't been reminded yet,
 * sends the reminder, and flips {@code reminderSent} to prevent duplicates.
 */
@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a");
    private static final int DEFAULT_REMINDER_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /** Fires every minute. Equivalent to the task_list spec's {@code @Scheduled(cron="0 * * * * *")}. */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendDueReminders() {
        int windowMinutes = settingsRepository.findById(Settings.SINGLETON_ID)
                .map(Settings::getReminderTime)
                .orElse(DEFAULT_REMINDER_MINUTES);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(windowMinutes);

        List<Booking> due = bookingRepository
                .findByStatusAndReminderSentFalseAndSlotAtBetween(
                        BookingStatus.CONFIRMED, now, windowEnd);

        for (Booking booking : due) {
            sendReminder(booking);
        }

        if (!due.isEmpty()) {
            log.info("Reminder scheduler: sent {} reminder(s)", due.size());
        }
    }

    private void sendReminder(Booking booking) {
        User student = userRepository.findById(booking.getStudentId()).orElse(null);
        if (student == null) return;

        emailService.sendReminder(
                student.getEmail(), student.getName(),
                booking.getProject(),
                DATE_FMT.format(booking.getSlotAt()),
                TIME_FMT.format(booking.getSlotAt()),
                booking.getMeetUrl());

        booking.setReminderSent(true);
        bookingRepository.save(booking);
    }
}
