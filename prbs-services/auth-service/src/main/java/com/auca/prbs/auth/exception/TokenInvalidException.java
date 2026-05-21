package com.auca.prbs.auth.exception;

import org.springframework.http.HttpStatus;

public class TokenInvalidException extends ApiException {
    public TokenInvalidException() {
        super("TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Token is invalid, expired, or revoked");
    }
}
