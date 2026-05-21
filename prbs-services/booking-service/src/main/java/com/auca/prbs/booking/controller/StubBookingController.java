package com.auca.prbs.booking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Returns 501 with a clear migration-pending message for every booking/availability route. */
@RestController
public class StubBookingController {

    @RequestMapping(
            value = {"/api/v1/bookings/**", "/api/v1/availability/**"},
            method = {RequestMethod.GET, RequestMethod.POST,
                      RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<?> notImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("code", "MIGRATION_PENDING",
                        "message", "Booking + availability migration is the next " +
                                "step after user-service. See prbs-services/MIGRATION.md."));
    }
}
