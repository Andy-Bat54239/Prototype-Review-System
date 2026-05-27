package com.auca.prbs.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "user-service", contextId = "settingsClient")
public interface SettingsClient {

    /** Internal lookup; user-service exposes a no-auth endpoint for cluster use. */
    @GetMapping("/internal/settings")
    SettingsView get();
}
