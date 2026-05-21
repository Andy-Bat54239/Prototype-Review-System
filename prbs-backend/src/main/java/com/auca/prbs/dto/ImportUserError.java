package com.auca.prbs.dto;

/** One per-row failure in a bulk CSV import. */
public record ImportUserError(
        int row,            // 1-based row number in the original CSV (excluding header)
        String email,       // the raw email value from that row, may be null/blank
        String message
) {}
