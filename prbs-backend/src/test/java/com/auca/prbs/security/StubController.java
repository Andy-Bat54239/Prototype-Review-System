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

    /**
     * Path under {@code /api/v1/users/**} that doesn't collide with the real
     * UserController's {@code GET /api/v1/users} list endpoint. Used to assert
     * that the ADMIN matcher gates the whole subtree.
     */
    @GetMapping("/api/v1/users/_stub")
    ResponseEntity<String> stubUsers() {
        return ResponseEntity.ok("ok");
    }

    /**
     * Distinctive path under /api/v1/auth/** that doesn't collide with the real
     * {@link com.auca.prbs.controller.AuthController}. Lets us assert the
     * permitAll rule covers the whole subtree without overlapping mappings.
     */
    @PostMapping("/api/v1/auth/_stub")
    ResponseEntity<String> stubAuth(@RequestBody(required = false) String body) {
        return ResponseEntity.ok("ok");
    }
}
