package com.auca.prbs.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal controller used only by {@link JwtAuthenticationFilterTest} to exercise the
 * security filter chain against representative routes from {@link SecurityConfig}.
 */
@RestController
class StubController {

    @GetMapping("/api/v1/bookings")
    ResponseEntity<String> bookings() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/api/v1/users")
    ResponseEntity<String> users() {
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/api/v1/auth/send-otp")
    ResponseEntity<String> sendOtp(@RequestBody(required = false) String body) {
        return ResponseEntity.ok("ok");
    }
}
