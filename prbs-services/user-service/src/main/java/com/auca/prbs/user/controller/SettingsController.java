package com.auca.prbs.user.controller;

import com.auca.prbs.user.dto.SettingsResponse;
import com.auca.prbs.user.dto.UpdateSettingsRequest;
import com.auca.prbs.user.entity.Settings;
import com.auca.prbs.user.repository.SettingsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only settings management. Partial update — null fields unchanged. */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsRepository settingsRepository;

    @GetMapping
    public ResponseEntity<SettingsResponse> get() {
        Settings s = settingsRepository.findById(Settings.SINGLETON_ID).orElseThrow();
        return ResponseEntity.ok(SettingsResponse.of(s));
    }

    @PatchMapping
    public ResponseEntity<SettingsResponse> update(@Valid @RequestBody UpdateSettingsRequest body) {
        Settings s = settingsRepository.findById(Settings.SINGLETON_ID).orElseThrow();
        if (body.otpExpiry()    != null) s.setOtpExpiry(body.otpExpiry());
        if (body.cancelWindow() != null) s.setCancelWindow(body.cancelWindow());
        if (body.reminderTime() != null) s.setReminderTime(body.reminderTime());
        return ResponseEntity.ok(SettingsResponse.of(settingsRepository.save(s)));
    }

    /**
     * In-cluster read for booking-service. Public on the internal network but
     * not exposed by the gateway. Same trust model as /api/v1/users/by-email.
     */
    @GetMapping("/internal")
    public SettingsResponse internalGet() {
        return SettingsResponse.of(settingsRepository.findById(Settings.SINGLETON_ID).orElseThrow());
    }
}
