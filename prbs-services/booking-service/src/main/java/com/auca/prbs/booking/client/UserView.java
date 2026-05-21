package com.auca.prbs.booking.client;

/** Subset of user-service's user fields that booking-service needs. */
public record UserView(Long id, String name, String email, String role, String status) {}
