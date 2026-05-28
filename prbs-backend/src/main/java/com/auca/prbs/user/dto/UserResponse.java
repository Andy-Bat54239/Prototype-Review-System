package com.auca.prbs.user.dto;

import com.auca.prbs.user.entity.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        String status,
        Instant createdAt
) {
    public static UserResponse of(User u) {
        return new UserResponse(
                u.getId(), u.getName(), u.getEmail(),
                u.getRole().name(), u.getStatus().name(),
                u.getCreatedAt());
    }
}
