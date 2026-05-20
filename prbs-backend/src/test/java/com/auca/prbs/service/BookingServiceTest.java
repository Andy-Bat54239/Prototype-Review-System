package com.auca.prbs.service;

import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import com.auca.prbs.repository.BookingRepository;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 6 — booking lifecycle email hooks.
 *
 * @Transactional rolls back DB changes after each test so booking rows don't
 * leak into the shared @SpringBootTest H2 instance; GreenMail captures emails
 * synchronously during send so assertions on delivery still work after rollback.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=3025"
})
class BookingServiceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Autowired BookingService bookingService;
    @Autowired BookingRepository bookingRepository;

    private static final Long ALICE_ID      = 1L;   // STUDENT, ACTIVE
    private static final Long SUPERVISOR_ID = 7L;   // SUPERVISOR

    @Test
    @Transactional
    void createBooking_persistsConfirmed_andEmailsStudentPlusSupervisor() throws Exception {
        int before = greenMail.getReceivedMessages().length;

        Booking saved = bookingService.createBooking(Booking.builder()
                .studentId(ALICE_ID)
                .supervisorId(SUPERVISOR_ID)
                .name("Alice Uwase")
                .groupNumber(3)
                .project("AI Crop Disease Monitor")
                .slotAt(LocalDateTime.now().plusDays(2))
                .meetUrl("https://meet.google.com/abc-defg-hij")
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(saved.isReminderSent()).isFalse();

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received.length - before).isEqualTo(2);

        var newMessages = Arrays.copyOfRange(received, before, received.length);
        var recipients = Arrays.stream(newMessages)
                .map(BookingServiceTest::recipient)
                .toList();
        assertThat(recipients).containsExactlyInAnyOrder(
                "alice@university.ac.rw",
                "supervisor@university.ac.rw");

        // Supervisor mail must include the booking event verb
        var supervisorMail = Arrays.stream(newMessages)
                .filter(m -> recipient(m).equals("supervisor@university.ac.rw"))
                .findFirst().orElseThrow();
        assertThat(supervisorMail.getSubject()).startsWith("Session scheduled");
        assertThat(GreenMailUtil.getBody(supervisorMail)).contains("AI Crop Disease Monitor");
    }

    @Test
    @Transactional
    void cancelBooking_flipsStatusAndAlertsSupervisorOnly() throws Exception {
        Booking created = bookingService.createBooking(Booking.builder()
                .studentId(ALICE_ID).supervisorId(SUPERVISOR_ID).name("Alice")
                .groupNumber(3).project("Test")
                .slotAt(LocalDateTime.now().plusDays(2))
                .meetUrl("https://meet.google.com/x").build());
        int afterCreate = greenMail.getReceivedMessages().length;

        Booking cancelled = bookingService.cancelBooking(created.getId());

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received.length - afterCreate).isEqualTo(1);

        MimeMessage cancelMail = received[received.length - 1];
        assertThat(cancelMail.getSubject()).startsWith("Session cancelled");
        assertThat(recipient(cancelMail)).isEqualTo("supervisor@university.ac.rw");
    }

    private static String recipient(MimeMessage m) {
        try { return m.getAllRecipients()[0].toString(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
