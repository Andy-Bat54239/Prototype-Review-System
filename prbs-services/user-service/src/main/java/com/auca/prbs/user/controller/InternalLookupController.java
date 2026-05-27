package com.auca.prbs.user.controller;

import com.auca.prbs.user.dto.SettingsResponse;
import com.auca.prbs.user.entity.Settings;
import com.auca.prbs.user.entity.User;
import com.auca.prbs.user.repository.SettingsRepository;
import com.auca.prbs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * In-cluster lookups consumed by other services (auth-service, booking-service).
 *
 * <p>Lives under {@code /internal/*} so the API gateway, which only routes
 * {@code /api/v1/*}, can't proxy these from the public internet. They're
 * reachable only inside the Docker / k8s network.
 *
 * <p>A real production deploy would still add mTLS or a shared service token —
 * path-prefix alone is defense in depth, not defense.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalLookupController {

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;

    @GetMapping("/users/by-email")
    public ResponseEntity<?> byEmail(@RequestParam("email") String email) {
        return userRepository.findByEmail(email)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(asMap(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/by-id/{id}")
    public ResponseEntity<?> byId(@PathVariable Long id) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(asMap(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/settings")
    public SettingsResponse settings() {
        return SettingsResponse.of(settingsRepository.findById(Settings.SINGLETON_ID).orElseThrow());
    }

    private static Map<String, Object> asMap(User u) {
        return Map.of(
                "id",     u.getId(),
                "name",   u.getName(),
                "email",  u.getEmail(),
                "role",   u.getRole().name(),
                "status", u.getStatus().name());
    }
}
