package com.auca.prbs.availability.exception;

import com.auca.prbs.config.ApiException;
import org.springframework.http.HttpStatus;

public class AvailabilityNotFoundException extends ApiException {
    public AvailabilityNotFoundException() {
        super("AVAILABILITY_NOT_FOUND", HttpStatus.NOT_FOUND, "Availability not found");
    }
}
