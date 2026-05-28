package com.auca.prbs.auth.exception;

import com.auca.prbs.config.ApiException;
import org.springframework.http.HttpStatus;

public class TokenInvalidException extends ApiException {
    public TokenInvalidException() {
        super("TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Token is invalid, expired, or revoked");
    }
}
