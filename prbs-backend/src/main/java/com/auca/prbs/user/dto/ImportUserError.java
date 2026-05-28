package com.auca.prbs.user.dto;

public record ImportUserError(int row, String email, String message) {}
