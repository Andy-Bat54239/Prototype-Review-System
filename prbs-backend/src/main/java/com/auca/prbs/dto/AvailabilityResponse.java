package com.auca.prbs.dto;

import com.auca.prbs.entity.Availability;
import com.auca.prbs.entity.User;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityResponse(
        Long id,
        Long supervisorId,
        String supervisorName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int durationMinutes,
        String meetUrl
) {
    public static AvailabilityResponse of(Availability av, User supervisor) {
        return new AvailabilityResponse(
                av.getId(),
                av.getSupervisorId(),
                supervisor == null ? null : supervisor.getName(),
                av.getDate(),
                av.getStartTime(),
                av.getEndTime(),
                av.getDurationMinutes(),
                av.getMeetUrl()
        );
    }
}
