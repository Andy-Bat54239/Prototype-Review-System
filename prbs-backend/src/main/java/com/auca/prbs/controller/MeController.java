package com.auca.prbs.controller;

import com.auca.prbs.dto.AuthResponse;
import com.auca.prbs.entity.User;
import com.auca.prbs.exception.UserNotFoundException;
import com.auca.prbs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the currently authenticated user's details. Useful for the React
 * frontend after refreshing a page (so it can re-derive the role + name without
 * decoding the JWT) and for verifying the JWT auth filter is wired correctly.
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<AuthResponse.UserSummary> me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return ResponseEntity.ok(new AuthResponse.UserSummary(
                user.getId(), user.getEmail(), user.getName(), user.getRole().name()));
    }
}
