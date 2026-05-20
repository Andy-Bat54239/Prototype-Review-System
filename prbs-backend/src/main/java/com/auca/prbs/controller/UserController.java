package com.auca.prbs.controller;

import com.auca.prbs.dto.UpdateUserStatusRequest;
import com.auca.prbs.dto.UserResponse;
import com.auca.prbs.entity.User;
import com.auca.prbs.exception.UserNotFoundException;
import com.auca.prbs.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin user management. SecurityConfig already restricts {@code /api/v1/users/**}
 * to the ADMIN role; no further checks needed here.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(UserResponse::of)
                .toList());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUserStatusRequest body) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setStatus(body.status());
        return ResponseEntity.ok(UserResponse.of(userRepository.save(user)));
    }
}
