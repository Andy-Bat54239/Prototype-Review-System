package com.auca.prbs.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * STUB — replaced by the full UserController in the next migration step.
 * Provides just enough for auth-service to function: the by-email lookup
 * with seeded users hardcoded.
 */
@RestController
@RequestMapping("/api/v1/users")
public class StubUserController {

    private static final Map<String, Map<String, Object>> SEEDED_USERS = Map.of(
            "alice@university.ac.rw",
                Map.of("id", 1L, "name", "Alice Uwase",      "email", "alice@university.ac.rw",
                       "role", "STUDENT",    "status", "ACTIVE"),
            "supervisor@university.ac.rw",
                Map.of("id", 7L, "name", "Dr. Sarah Mensah", "email", "supervisor@university.ac.rw",
                       "role", "SUPERVISOR", "status", "ACTIVE"),
            "admin@university.ac.rw",
                Map.of("id", 8L, "name", "Admin User",       "email", "admin@university.ac.rw",
                       "role", "ADMIN",      "status", "ACTIVE"),
            "chidi@university.ac.rw",
                Map.of("id", 6L, "name", "Chidi Okafor",     "email", "chidi@university.ac.rw",
                       "role", "STUDENT",    "status", "INACTIVE")
    );

    @GetMapping("/by-email")
    public ResponseEntity<?> byEmail(@RequestParam String email) {
        return Optional.ofNullable(SEEDED_USERS.get(email))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> notImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("code", "MIGRATION_PENDING",
                        "message", "Full UserController will be migrated next. " +
                                "See prbs-services/MIGRATION.md."));
    }
}
