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

    @GetMapping("/internal/users/by-email")
    Optional<UserView> findByEmail(@RequestParam("email") String email);

    /** In-cluster lookup by id — used to resolve student/supervisor for emails + responses. */
    @GetMapping("/internal/users/by-id/{id}")
    Optional<UserView> findById(@org.springframework.web.bind.annotation.PathVariable("id") Long id);
}
