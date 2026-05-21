package com.auca.prbs.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "notification-service")
public interface NotificationClient {
    @PostMapping("/internal/emails/otp")
    void sendOtp(@RequestBody Map<String, String> payload);
}
