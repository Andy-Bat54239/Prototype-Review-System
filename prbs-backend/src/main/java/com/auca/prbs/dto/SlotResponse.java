package com.auca.prbs.dto;

import java.time.LocalTime;

/** One slot inside a supervisor's availability window. {@code available=false} means another booking holds it. */
public record SlotResponse(
        LocalTime time,
        boolean available
) {}
