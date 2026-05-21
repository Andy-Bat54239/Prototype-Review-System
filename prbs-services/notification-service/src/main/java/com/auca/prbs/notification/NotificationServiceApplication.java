package com.auca.prbs.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

/**
 * Owns the email templates and SMTP send. Has no DB and no auth — it's a private
 * service reachable only inside the cluster (the gateway doesn't route /internal/*).
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
