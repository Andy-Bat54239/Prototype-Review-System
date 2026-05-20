package com.auca.prbs.exception;

import org.springframework.http.HttpStatus;

public class SlotConflictException extends ApiException {
    public SlotConflictException() {
        super("SLOT_CONFLICT", HttpStatus.CONFLICT, "Another booking already holds this slot");
    }
}
