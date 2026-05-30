package com.auca.prbs.user.controller;

import com.auca.prbs.user.dto.UserSummary;
import com.auca.prbs.user.entity.User;
import com.auca.prbs.user.exception.UserNotFoundException;
import com.auca.prbs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Currently-authenticated user's summary. Frontend uses this to render role-aware UI. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<UserSummary> me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return ResponseEntity.ok(new UserSummary(
                user.getId(), user.getEmail(), user.getName(), user.getRole().name()));
    }
}
