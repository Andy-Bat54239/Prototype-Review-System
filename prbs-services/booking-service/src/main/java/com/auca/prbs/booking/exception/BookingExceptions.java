package com.auca.prbs.booking.exception;

import org.springframework.http.HttpStatus;

/** Grouped to keep the file count down; each is a thin marker class. */
public final class BookingExceptions {
    private BookingExceptions() {}

    public static class BookingNotFoundException extends ApiException {
        public BookingNotFoundException() { super("BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND, "Booking not found"); }
    }

    public static class AvailabilityNotFoundException extends ApiException {
        public AvailabilityNotFoundException() { super("AVAILABILITY_NOT_FOUND", HttpStatus.NOT_FOUND, "Availability not found"); }
    }

    public static class SlotConflictException extends ApiException {
        public SlotConflictException() { super("SLOT_CONFLICT", HttpStatus.CONFLICT, "Another booking already holds this slot"); }
    }

    public static class SlotOutsideAvailabilityException extends ApiException {
        public SlotOutsideAvailabilityException(String detail) { super("SLOT_OUT_OF_RANGE", HttpStatus.BAD_REQUEST, detail); }
    }

    public static class CancelWindowExceededException extends ApiException {
        public CancelWindowExceededException(int windowMinutes) {
            super("CANCEL_WINDOW_EXCEEDED", HttpStatus.CONFLICT,
                    "Cancellations must be at least " + windowMinutes + " minutes before the session");
        }
    }

    public static class BookingAccessDeniedException extends ApiException {
        public BookingAccessDeniedException() { super("BOOKING_FORBIDDEN", HttpStatus.FORBIDDEN,
                "Only the booking's student or supervisor can perform this action"); }
    }

    public static class AvailabilityAccessDeniedException extends ApiException {
        public AvailabilityAccessDeniedException() { super("AVAILABILITY_FORBIDDEN", HttpStatus.FORBIDDEN,
                "Only the owning supervisor can modify this availability"); }
    }

    public static class InvalidBookingStatusException extends ApiException {
        public InvalidBookingStatusException(String detail) { super("INVALID_BOOKING_STATUS", HttpStatus.BAD_REQUEST, detail); }
    }

    public static class UserNotFoundException extends ApiException {
        public UserNotFoundException() { super("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found"); }
    }
}
