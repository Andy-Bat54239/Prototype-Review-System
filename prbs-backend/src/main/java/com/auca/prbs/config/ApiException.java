package com.auca.prbs.config;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for typed, code-bearing API errors. Lives in {@code config} (not in
 * any one business package) because every package throws it.
 *
 * <p>The {@link GlobalExceptionHandler} converts these into {@code {code, message}}
 * JSON responses with the {@link #status}.
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
