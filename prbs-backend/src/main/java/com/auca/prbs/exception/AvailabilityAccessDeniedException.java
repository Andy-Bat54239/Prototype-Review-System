package com.auca.prbs.exception;

import org.springframework.http.HttpStatus;

public class AvailabilityAccessDeniedException extends ApiException {
    public AvailabilityAccessDeniedException() {
        super("AVAILABILITY_FORBIDDEN", HttpStatus.FORBIDDEN,
                "Only the owning supervisor can modify this availability");
    }
}
