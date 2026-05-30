package com.auca.prbs.auth.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * auth-service owns OTP delivery directly — OTP emails are the only thing
 * blocking the login flow, so we don't want them gated on notification-service
 * availability. Other email types (booking confirmation, reminder, etc.) live
 * in notification-service.
 */
@Service
@RequiredArgsConstructor
public class OtpEmailSender {

    private static final Logger log = LoggerFactory.getLogger(OtpEmailSender.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from:noreply@auca.ac.rw}")
    private String from;

    public void sendOtp(String to, String name, String otp) {
        try {
            var message = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Your PRBS Login Code");
            helper.setText(html(name, otp), true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            // Swallow the failure so /send-otp stays 200 (anti-enumeration).
            // The OTP record is already in the DB; only the email delivery failed.
            // Ops sees the error in the log; the user just doesn't get an email.
            log.error("OTP email delivery failed for {} (SMTP/template error). " +
                      "Code remains valid in DB — investigate mail config.", to, e);
        }
    }

    private String html(String name, String otp) {
        return """
            <div style="font-family:DM Sans,sans-serif;max-width:520px;margin:auto">
              <div style="background:#0F2755;padding:28px 32px;border-radius:12px 12px 0 0">
                <h1 style="color:white;font-size:20px;margin:0">AUCA · PRBS</h1>
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
              </div>
            </div>
            """.formatted(HtmlUtils.htmlEscape(name == null ? "" : name),
                          HtmlUtils.htmlEscape(otp == null ? "" : otp));
    }
}
