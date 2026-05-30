package com.auca.prbs.booking.exception;

import com.auca.prbs.config.ApiException;
import org.springframework.http.HttpStatus;

public class CancelWindowExceededException extends ApiException {
    public CancelWindowExceededException(int windowMinutes) {
        super("CANCEL_WINDOW_EXCEEDED", HttpStatus.CONFLICT,
                "Cancellations must be at least " + windowMinutes + " minutes before the session");
    }
}
