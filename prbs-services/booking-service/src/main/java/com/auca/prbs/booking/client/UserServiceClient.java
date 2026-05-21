package com.auca.prbs.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Looks up user data live from user-service. Booking-service does not duplicate
 * user records — single source of truth lives in user-service.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/by-email")
    Optional<UserView> findByEmail(@RequestParam("email") String email);

    /**
     * Looks up a user by id. user-service exposes this on a future
     * {@code /api/v1/users/{id}} admin endpoint; booking-service hits it
     * via an internal route that doesn't require an admin token (in-cluster only).
     */
    @GetMapping("/api/v1/users/by-id/{id}")
    Optional<UserView> findById(@org.springframework.web.bind.annotation.PathVariable("id") Long id);
}
