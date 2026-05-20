package com.auca.prbs.exception;

import org.springframework.http.HttpStatus;

public class UserInactiveException extends ApiException {
    public UserInactiveException() {
        super("USER_INACTIVE", HttpStatus.FORBIDDEN, "This account is not active");
    }
}
