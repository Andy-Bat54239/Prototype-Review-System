package com.auca.prbs.auth.client;

/**
 * Subset of user-service's User entity that auth-service needs.
 * Field names match user-service's response JSON.
 */
public record UserView(
        Long id,
        String name,
        String email,
        String role,
        String status
) {}
