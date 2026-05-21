package com.auca.prbs.auth.exception;

import org.springframework.http.HttpStatus;

public class OtpInvalidException extends ApiException {
    public OtpInvalidException() {
        super("OTP_INVALID", HttpStatus.UNAUTHORIZED, "Invalid or expired code");
    }
}
