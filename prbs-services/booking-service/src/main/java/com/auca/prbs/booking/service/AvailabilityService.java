package com.auca.prbs.booking.service;

import com.auca.prbs.booking.dto.SlotResponse;
import com.auca.prbs.booking.entity.Availability;
import com.auca.prbs.booking.entity.BookingStatus;
import com.auca.prbs.booking.exception.BookingExceptions.AvailabilityNotFoundException;
import com.auca.prbs.booking.repository.AvailabilityRepository;
import com.auca.prbs.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates the slot grid the student picker renders. Marks any slot that's
 * already taken by a CONFIRMED booking as {@code taken=true}.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;

    public List<SlotResponse> getSlotsForAvailability(Long availabilityId) {
        Availability av = availabilityRepository.findById(availabilityId)
                .orElseThrow(AvailabilityNotFoundException::new);

        List<LocalTime> slotTimes = generateSlotTimes(av);

        Set<LocalTime> taken = new HashSet<>();
        bookingRepository.findBySupervisorIdOrderBySlotAtDesc(av.getSupervisorId()).forEach(b -> {
            if (b.getStatus() == BookingStatus.CONFIRMED && b.getSlotAt().toLocalDate().equals(av.getDate())) {
                taken.add(b.getSlotAt().toLocalTime());
            }
        });

        return slotTimes.stream()
                .map(t -> new SlotResponse(t, taken.contains(t)))
                .toList();
    }

    /** Equivalent of generateSlots() in prbs-app/src/data.js — same step math. */
    static List<LocalTime> generateSlotTimes(Availability av) {
        List<LocalTime> out = new ArrayList<>();
        int cur = av.getStartTime().getHour() * 60 + av.getStartTime().getMinute();
        int end = av.getEndTime().getHour()   * 60 + av.getEndTime().getMinute();
        while (cur + av.getDurationMinutes() <= end) {
            out.add(LocalTime.of(cur / 60, cur % 60));
            cur += av.getDurationMinutes();
        }
        return out;
    }
}
