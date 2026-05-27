package com.auca.prbs.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Synchronous lookup against user-service. Eureka resolves the logical name
 * {@code user-service} to a concrete instance. Calls land on the
 * {@code /internal/*} path tree which the API gateway does not route — only
 * reachable inside the cluster.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/internal/users/by-email")
    Optional<UserView> findByEmail(@RequestParam("email") String email);
}
