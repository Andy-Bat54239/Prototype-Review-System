package com.auca.prbs.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {
    public record UserSummary(Long id, String email, String name, String role) {}
}
