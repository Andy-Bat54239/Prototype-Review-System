package com.auca.prbs.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    public void sendOtp(String toEmail, String recipientName, String otpCode) {
        send(toEmail, "Your PRBS Login Code", buildOtpHtml(recipientName, otpCode));
    }

    public void sendBookingConfirmation(String toEmail, String recipientName,
                                        String project, String date,
                                        String time, String meetUrl) {
        send(toEmail, "Booking Confirmed — " + date,
                buildConfirmationHtml(recipientName, project, date, time, meetUrl));
    }

    public void sendReminder(String toEmail, String recipientName,
                             String project, String date,
                             String time, String meetUrl) {
        send(toEmail, "Reminder: Review Session Today at " + time,
                buildReminderHtml(recipientName, project, date, time, meetUrl));
    }

    private void send(String to, String subject, String html) {
        try {
            var message = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailDeliveryException("Failed to send email to " + to, e);
        }
    }

    private String buildOtpHtml(String name, String otp) {
        return """
            <div style="font-family:DM Sans,sans-serif;max-width:520px;margin:auto">
              <div style="background:#0F2755;padding:28px 32px;border-radius:12px 12px 0 0">
                <h1 style="color:white;font-size:20px;margin:0">AUCA · PRBS</h1>
                <p style="color:rgba(255,255,255,0.6);font-size:13px;margin:4px 0 0">
                  Prototype Review Booking System
                </p>
              </div>
              <div style="background:#ffffff;padding:32px;border:1px solid #EDE9E2;
                          border-top:none;border-radius:0 0 12px 12px">
                <p style="color:#1C1814;font-size:15px">Hi %s,</p>
                <p style="color:#7A7069;font-size:14px">
                  Use the code below to sign in. It expires in 10 minutes.
                </p>
                <div style="background:#E5EDF8;border-radius:10px;padding:20px;
                            text-align:center;margin:24px 0">
                  <span style="font-size:36px;font-weight:700;color:#1D5BAF;
                               letter-spacing:10px">%s</span>
                </div>
                <p style="color:#B8AFA2;font-size:12px">
                  If you did not request this code, ignore this email.
                </p>
              </div>
            </div>
            """.formatted(esc(name), esc(otp));
    }

    private String buildConfirmationHtml(String name, String project,
                                         String date, String time, String meetUrl) {
        return """
            <div style="font-family:DM Sans,sans-serif;max-width:520px;margin:auto">
              <div style="background:#0F2755;padding:28px 32px;border-radius:12px 12px 0 0">
                <h1 style="color:white;font-size:20px;margin:0">Booking Confirmed</h1>
                <p style="color:rgba(255,255,255,0.6);font-size:13px;margin:4px 0 0">AUCA · PRBS</p>
              </div>
              <div style="background:#ffffff;padding:32px;border:1px solid #EDE9E2;
                          border-top:none;border-radius:0 0 12px 12px">
                <p style="color:#1C1814;font-size:15px">Hi %s,</p>
                <p style="color:#7A7069;font-size:14px">
                  Your review session has been booked successfully.
                </p>
                <table style="width:100%%;border-collapse:collapse;margin:20px 0">
                  <tr><td style="padding:10px;background:#F8F5F0;border-radius:8px;
                                 font-size:13px;color:#7A7069;width:40%%">Project</td>
                      <td style="padding:10px;font-size:14px;font-weight:600;color:#1C1814">%s</td></tr>
                  <tr><td style="padding:10px;font-size:13px;color:#7A7069">Date</td>
                      <td style="padding:10px;font-size:14px;font-weight:600;color:#1C1814">%s</td></tr>
                  <tr><td style="padding:10px;background:#F8F5F0;font-size:13px;color:#7A7069">Time</td>
                      <td style="padding:10px;background:#F8F5F0;font-size:14px;
                                 font-weight:600;color:#1C1814">%s</td></tr>
                </table>
                <a href="%s" style="display:inline-block;background:#1D5BAF;color:white;
                                    padding:12px 24px;border-radius:8px;text-decoration:none;
                                    font-weight:600;font-size:14px">
                  Join Google Meet →
                </a>
              </div>
            </div>
            """.formatted(esc(name), esc(project), esc(date), esc(time), escAttr(meetUrl));
    }

    private String buildReminderHtml(String name, String project,
                                     String date, String time, String meetUrl) {
        return """
            <div style="font-family:DM Sans,sans-serif;max-width:520px;margin:auto">
              <div style="background:#0F2755;padding:28px 32px;border-radius:12px 12px 0 0">
                <h1 style="color:white;font-size:20px;margin:0">Session Reminder</h1>
                <p style="color:rgba(255,255,255,0.6);font-size:13px;margin:4px 0 0">AUCA · PRBS</p>
              </div>
              <div style="background:#ffffff;padding:32px;border:1px solid #EDE9E2;
                          border-top:none;border-radius:0 0 12px 12px">
                <p style="color:#1C1814;font-size:15px">Hi %s,</p>
                <p style="color:#7A7069;font-size:14px">
                  This is a reminder that your review session is coming up soon.
                </p>
                <div style="background:#E5EDF8;border-radius:10px;padding:16px 20px;margin:20px 0">
                  <p style="margin:0;font-size:14px;font-weight:600;color:#1D5BAF">%s</p>
                  <p style="margin:6px 0 0;font-size:13px;color:#7A7069">%s at %s</p>
                </div>
                <a href="%s" style="display:inline-block;background:#1D5BAF;color:white;
                                    padding:12px 24px;border-radius:8px;text-decoration:none;
                                    font-weight:600;font-size:14px">
                  Join Google Meet →
                </a>
              </div>
            </div>
            """.formatted(esc(name), esc(project), esc(date), esc(time), escAttr(meetUrl));
    }

    /** HTML-escape text content. Empty string for null inputs to keep templates well-formed. */
    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    /**
     * Stricter escape for values placed inside an attribute (here: an href).
     * Only `https://`, `http://`, and `mailto:` URLs are allowed through verbatim
     * after HTML-escaping; anything else (e.g. `javascript:`) is replaced with `#`.
     */
    private static String escAttr(String url) {
        if (url == null) return "#";
        String lower = url.trim().toLowerCase();
        boolean allowed = lower.startsWith("https://")
                       || lower.startsWith("http://")
                       || lower.startsWith("mailto:");
        return allowed ? HtmlUtils.htmlEscape(url) : "#";
    }
}
