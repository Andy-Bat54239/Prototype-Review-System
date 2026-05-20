package com.auca.prbs.service;

import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import com.auca.prbs.repository.BookingRepository;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 7 — cron tick. Calls {@link ReminderScheduler#sendDueReminders()}
 * directly so test timing doesn't depend on @Scheduled firing.
 *
 * @Transactional keeps the seeded bookings invisible to the real @Scheduled
 * thread (different transaction) and rolls back cleanup.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=3025"
})
class ReminderSchedulerTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Autowired ReminderScheduler reminderScheduler;
    @Autowired BookingRepository bookings;

    private static final Long ALICE_ID      = 1L;
    private static final Long SUPERVISOR_ID = 7L;

    @Test
    @Transactional
    void sendDueReminders_emailsOnlyConfirmedNotRemindedInsideWindow() throws Exception {
        int before = greenMail.getReceivedMessages().length;
        LocalDateTime now = LocalDateTime.now();

        // Window per settings.reminder_time is 30 minutes by default.
        Booking dueSoon          = persist(now.plusMinutes(15), BookingStatus.CONFIRMED, false);
        Booking alreadyReminded  = persist(now.plusMinutes(20), BookingStatus.CONFIRMED, true);
        Booking cancelled        = persist(now.plusMinutes(10), BookingStatus.CANCELLED, false);
        Booking outOfWindow      = persist(now.plusHours(2),    BookingStatus.CONFIRMED, false);

        reminderScheduler.sendDueReminders();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received.length - before)
                .as("exactly one reminder email should fire")
                .isEqualTo(1);

        MimeMessage reminder = received[received.length - 1];
        assertThat(reminder.getSubject()).startsWith("Reminder:");
        assertThat(reminder.getAllRecipients()[0].toString()).isEqualTo("alice@university.ac.rw");

        // Only the in-window booking flipped reminder_sent
        assertThat(bookings.findById(dueSoon.getId()).orElseThrow().isReminderSent()).isTrue();
        assertThat(bookings.findById(alreadyReminded.getId()).orElseThrow().isReminderSent()).isTrue(); // unchanged
        assertThat(bookings.findById(cancelled.getId()).orElseThrow().isReminderSent()).isFalse();
        assertThat(bookings.findById(outOfWindow.getId()).orElseThrow().isReminderSent()).isFalse();
    }

    @Test
    @Transactional
    void sendDueReminders_isIdempotent_secondTickSendsNothing() {
        int before = greenMail.getReceivedMessages().length;
        persist(LocalDateTime.now().plusMinutes(15), BookingStatus.CONFIRMED, false);

        reminderScheduler.sendDueReminders();
        int afterFirst = greenMail.getReceivedMessages().length;
        assertThat(afterFirst - before).isEqualTo(1);

        reminderScheduler.sendDueReminders();
        int afterSecond = greenMail.getReceivedMessages().length;
        assertThat(afterSecond - afterFirst).as("second tick must not re-send").isEqualTo(0);
    }

    private Booking persist(LocalDateTime slotAt, BookingStatus status, boolean reminded) {
        return bookings.save(Booking.builder()
                .studentId(ALICE_ID).supervisorId(SUPERVISOR_ID)
                .name("Alice Uwase").groupNumber(3).project("Test Project")
                .slotAt(slotAt).status(status)
                .meetUrl("https://meet.google.com/test")
                .reminderSent(reminded).build());
    }
}
