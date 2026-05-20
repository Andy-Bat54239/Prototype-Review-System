package com.auca.prbs.service;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots a real SMTP server in-process via GreenMail (port 3025, the JUnit default).
 * Spring's JavaMailSender targets that port. The same code path that talks to MailHog
 * in dev runs here — only the destination differs. To visually inspect rendered HTML,
 * start MailHog locally and re-run with `-Dspring.mail.port=1025`.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=3025",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "mail.from=noreply@auca.ac.rw",
        "jwt.secret=prbs-local-dev-secret-must-be-at-least-32-chars"
})
class EmailServiceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Autowired EmailService emailService;

    @Test
    void sendOtp_deliversWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                emailService.sendOtp("alice@university.ac.rw", "Alice Uwase", "483921"));

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("Your PRBS Login Code", received[0].getSubject());
        assertTrue(GreenMailUtil.getBody(received[0]).contains("483921"));
    }

    @Test
    void sendBookingConfirmation_deliversWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                emailService.sendBookingConfirmation(
                        "alice@university.ac.rw", "Alice Uwase",
                        "AI Crop Disease Monitor", "Monday, 25 April 2026",
                        "9:00 AM", "https://meet.google.com/abc-defg-hij"));

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertTrue(received[0].getSubject().startsWith("Booking Confirmed"));
        String body = GreenMailUtil.getBody(received[0]);
        assertTrue(body.contains("AI Crop Disease Monitor"));
        assertTrue(body.contains("https://meet.google.com/abc-defg-hij"));
    }

    @Test
    void sendReminder_deliversWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                emailService.sendReminder(
                        "alice@university.ac.rw", "Alice Uwase",
                        "AI Crop Disease Monitor", "Monday, 25 April 2026",
                        "9:00 AM", "https://meet.google.com/abc-defg-hij"));

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertTrue(received[0].getSubject().startsWith("Reminder:"));
    }
}
