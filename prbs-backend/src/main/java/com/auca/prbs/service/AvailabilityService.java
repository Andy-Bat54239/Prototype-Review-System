package com.auca.prbs.service;

import com.auca.prbs.dto.SlotResponse;
import com.auca.prbs.entity.Availability;
import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.BookingStatus;
import com.auca.prbs.exception.AvailabilityNotFoundException;
import com.auca.prbs.repository.AvailabilityRepository;
import com.auca.prbs.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Slot algebra over a supervisor's {@link Availability} window. Port of
 * {@code generateSlots()} in {@code prbs-app/src/data.js} — same semantics,
 * server-authoritative.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;

    /**
     * Walks the availability window in {@code durationMinutes} steps and yields
     * each canonical slot start time. The last slot must <i>end</i> at or before
     * {@code endTime}; partial slots are discarded.
     */
    public List<LocalTime> generateSlots(Availability av) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime cur = av.getStartTime();
        int duration = av.getDurationMinutes();
        while (!cur.plusMinutes(duration).isAfter(av.getEndTime())) {
            slots.add(cur);
            cur = cur.plusMinutes(duration);
        }
        return slots;
    }

    /**
     * The slot list with availability flags. A slot is {@code available=false} if
     * a non-cancelled booking already holds {@code (supervisorId, date+slotTime)}.
     */
    @Transactional(readOnly = true)
    public List<SlotResponse> getSlotsForAvailability(Long availabilityId) {
        Availability av = availabilityRepository.findById(availabilityId)
                .orElseThrow(AvailabilityNotFoundException::new);

        Set<LocalTime> taken = bookingRepository
                .findBySupervisorIdAndSlotAtBetween(
                        av.getSupervisorId(),
                        av.getDate().atStartOfDay(),
                        av.getDate().atTime(23, 59, 59))
                .stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(b -> b.getSlotAt().toLocalTime())
                .collect(Collectors.toSet());

        return generateSlots(av).stream()
                .map(t -> new SlotResponse(t, !taken.contains(t)))
                .toList();
    }
}
