package com.auca.prbs.booking.dto;

import com.auca.prbs.booking.client.UserView;
import com.auca.prbs.booking.entity.Availability;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityResponse(
        Long id,
        Long supervisorId, String supervisorName,
        LocalDate date,
        LocalTime startTime, LocalTime endTime,
        Integer durationMinutes,
        String meetUrl
) {
    public static AvailabilityResponse of(Availability a, UserView supervisor) {
        return new AvailabilityResponse(
                a.getId(),
                a.getSupervisorId(),
                supervisor == null ? null : supervisor.name(),
                a.getDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getDurationMinutes(),
                a.getMeetUrl()
        );
    }
}
