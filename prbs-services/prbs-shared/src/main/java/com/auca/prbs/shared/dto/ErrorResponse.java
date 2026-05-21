package com.auca.prbs.shared.dto;

/**
 * Unified error envelope across all services. Code is a stable machine-readable
 * identifier (e.g. {@code USER_NOT_FOUND}); message is human-readable.
 */
public record ErrorResponse(String code, String message) {}
