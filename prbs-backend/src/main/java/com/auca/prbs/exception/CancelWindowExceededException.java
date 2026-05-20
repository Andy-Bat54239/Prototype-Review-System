package com.auca.prbs.exception;

import org.springframework.http.HttpStatus;

public class CancelWindowExceededException extends ApiException {
    public CancelWindowExceededException(int windowMinutes) {
        super("CANCEL_WINDOW_EXCEEDED", HttpStatus.CONFLICT,
                "Cancellations must be at least " + windowMinutes + " minutes before the session");
    }
}
