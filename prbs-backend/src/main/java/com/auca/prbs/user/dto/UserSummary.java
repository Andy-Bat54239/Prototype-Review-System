package com.auca.prbs.user.dto;

/** Returned by /api/v1/me. Frontend uses this to render role-aware UI without decoding the JWT. */
public record UserSummary(Long id, String email, String name, String role) {}
