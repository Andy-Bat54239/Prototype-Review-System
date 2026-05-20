package com.auca.prbs.exception;

import org.springframework.http.HttpStatus;

public class OtpInvalidException extends ApiException {
    public OtpInvalidException() {
        super("OTP_INVALID", HttpStatus.UNAUTHORIZED, "Invalid or expired code");
    }
}
