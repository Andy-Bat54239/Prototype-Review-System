package com.auca.prbs.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Booking-service fires email side-effects through this client. The actual
 * email rendering lives in notification-service so we can swap templates and
 * SMTP providers without touching booking logic.
 */
@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/internal/emails/booking-confirmation")
    void sendBookingConfirmation(@RequestBody Map<String, String> payload);

    @PostMapping("/internal/emails/supervisor-alert")
    void sendSupervisorAlert(@RequestBody Map<String, String> payload);

    @PostMapping("/internal/emails/reminder")
    void sendReminder(@RequestBody Map<String, String> payload);
}
