package com.auca.prbs.booking.exception;

import com.auca.prbs.config.ApiException;
import org.springframework.http.HttpStatus;

public class SlotOutsideAvailabilityException extends ApiException {
    public SlotOutsideAvailabilityException(String detail) {
        super("SLOT_OUT_OF_RANGE", HttpStatus.BAD_REQUEST, detail);
    }
}
