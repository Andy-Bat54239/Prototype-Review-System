package com.auca.prbs.controller;

import com.auca.prbs.dto.SettingsResponse;
import com.auca.prbs.dto.UpdateSettingsRequest;
import com.auca.prbs.entity.Settings;
import com.auca.prbs.repository.SettingsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only settings management. SecurityConfig restricts {@code /api/v1/settings/**}
 * to the ADMIN role. Partial updates: any field left null in the request is unchanged.
 */
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
}
