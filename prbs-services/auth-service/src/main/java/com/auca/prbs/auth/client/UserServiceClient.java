package com.auca.prbs.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Synchronous lookup against user-service. Eureka resolves the logical name
 * {@code user-service} to a concrete instance.
 *
 * <p>The user-service must expose {@code GET /api/v1/users/by-email?email=...}
 * as an INTERNAL endpoint (no auth required, but only reachable in-cluster).
 */
@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/by-email")
    Optional<UserView> findByEmail(@RequestParam("email") String email);
}
