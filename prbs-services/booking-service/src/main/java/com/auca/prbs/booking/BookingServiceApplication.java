package com.auca.prbs.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

/** STUB — full BookingController, AvailabilityController, BookingService follow. */
@SpringBootApplication(
        scanBasePackages = "com.auca.prbs.booking",
        exclude = SecurityAutoConfiguration.class)
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
