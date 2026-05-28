package com.auca.prbs.availability.dto;

import java.time.LocalTime;

/** One row of the slot grid the student picker shows. */
public record SlotResponse(LocalTime time, boolean taken) {}
