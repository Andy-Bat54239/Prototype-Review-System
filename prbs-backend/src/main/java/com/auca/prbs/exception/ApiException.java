package com.auca.prbs.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base for typed, code-bearing API errors. The global handler turns these into
 * {@code {code, message}} JSON responses with the {@link #status}.
 */
@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
