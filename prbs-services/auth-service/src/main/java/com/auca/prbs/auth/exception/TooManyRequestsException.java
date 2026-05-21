package com.auca.prbs.auth.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException() {
        super("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests for this email; try again in a minute");
    }
}
