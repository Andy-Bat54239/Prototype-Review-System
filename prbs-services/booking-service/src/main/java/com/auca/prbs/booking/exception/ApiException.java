package com.auca.prbs.booking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    public ApiException(String code, HttpStatus status, String message) {
        super(message); this.code = code; this.status = status;
    }
}
