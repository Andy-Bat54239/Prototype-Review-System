package com.auca.prbs.notification.controller;

import com.auca.prbs.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * In-cluster endpoints called by auth-service / booking-service. The gateway
 * does not route /internal/* so these are unreachable from outside the cluster.
 *
 * <p>Each handler accepts a flat {@code Map<String,String>} payload — keeps the
 * cross-service DTOs out of prbs-shared and lets each caller include only the
 * fields its template needs.
 */
@RestController
@RequestMapping("/internal/emails")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    @PostMapping("/otp")
    public ResponseEntity<Void> otp(@RequestBody Map<String, String> p) {
        emailService.sendOtp(p.get("email"), p.get("name"), p.get("code"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/booking-confirmation")
    public ResponseEntity<Void> bookingConfirmation(@RequestBody Map<String, String> p) {
        emailService.sendBookingConfirmation(
                p.get("studentEmail"), p.get("studentName"),
                p.get("project"), p.get("date"), p.get("time"), p.get("meetUrl"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/supervisor-alert")
    public ResponseEntity<Void> supervisorAlert(@RequestBody Map<String, String> p) {
        emailService.sendSupervisorAlert(
                p.get("supervisorEmail"), p.get("supervisorName"),
                p.get("event"), p.get("studentName"),
                p.get("project"), p.get("date"), p.get("time"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reminder")
    public ResponseEntity<Void> reminder(@RequestBody Map<String, String> p) {
        emailService.sendReminder(
                p.get("studentEmail"), p.get("studentName"),
                p.get("project"), p.get("date"), p.get("time"), p.get("meetUrl"));
        return ResponseEntity.ok().build();
    }
}
