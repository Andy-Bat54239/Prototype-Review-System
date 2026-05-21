package com.auca.prbs.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

/**
 * STUB. Migration of UserController, SettingsController, MeController, and the
 * CSV bulk-import endpoint is the next session's work. Today this service
 * registers with Eureka and exposes one in-cluster endpoint that auth-service
 * needs to function ({@code GET /api/v1/users/by-email}).
 */
@SpringBootApplication(
        scanBasePackages = "com.auca.prbs.user",
        exclude = SecurityAutoConfiguration.class)
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
