package com.auca.prbs.user.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException() {
        super("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "No user with that email");
    }
}
