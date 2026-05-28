package com.auca.prbs.availability.dto;

import com.auca.prbs.availability.entity.Availability;
import com.auca.prbs.user.entity.User;

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
    public static AvailabilityResponse of(Availability a, User supervisor) {
        return new AvailabilityResponse(
                a.getId(),
                a.getSupervisorId(),
                supervisor == null ? null : supervisor.getName(),
                a.getDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getDurationMinutes(),
                a.getMeetUrl()
        );
    }
}
