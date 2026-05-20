package com.auca.prbs.controller;

import com.auca.prbs.dto.AvailabilityRequest;
import com.auca.prbs.dto.AvailabilityResponse;
import com.auca.prbs.dto.SlotResponse;
import com.auca.prbs.entity.Availability;
import com.auca.prbs.entity.User;
import com.auca.prbs.exception.AvailabilityAccessDeniedException;
import com.auca.prbs.exception.AvailabilityNotFoundException;
import com.auca.prbs.repository.AvailabilityRepository;
import com.auca.prbs.repository.UserRepository;
import com.auca.prbs.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;

    /**
     * List upcoming availability blocks. Without {@code supervisorId}, returns
     * every supervisor's availability from {@code from} onward (defaults to today).
     */
    @GetMapping
    public List<AvailabilityResponse> list(
            @RequestParam(required = false) Long supervisorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from
    ) {
        LocalDate fromDate = from != null ? from : LocalDate.now();
        List<Availability> rows = (supervisorId != null)
                ? availabilityRepository.findBySupervisorIdAndDateGreaterThanEqualOrderByDateAsc(supervisorId, fromDate)
                : availabilityRepository.findByDateGreaterThanEqualOrderByDateAsc(fromDate);

        // Hydrate supervisor names in one pass.
        Map<Long, User> cache = new HashMap<>();
        return rows.stream()
                .map(av -> AvailabilityResponse.of(av,
                        cache.computeIfAbsent(av.getSupervisorId(),
                                id -> userRepository.findById(id).orElse(null))))
                .toList();
    }

    /** Supervisor declares a new availability block. Path-gated to SUPERVISOR by SecurityConfig. */
    @PostMapping
    public ResponseEntity<AvailabilityResponse> create(Authentication auth,
                                                       @Valid @RequestBody AvailabilityRequest body) {
        Long supervisorId = (Long) auth.getPrincipal();
        User supervisor = userRepository.findById(supervisorId).orElseThrow();

        Availability saved = availabilityRepository.save(Availability.builder()
                .supervisorId(supervisorId)
                .date(body.date())
                .startTime(body.startTime())
                .endTime(body.endTime())
                .durationMinutes(body.durationMinutes())
                .meetUrl(body.meetUrl())
                .build());

        return ResponseEntity.ok(AvailabilityResponse.of(saved, supervisor));
    }

    /** Owning supervisor deletes an availability block. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        Availability av = availabilityRepository.findById(id)
                .orElseThrow(AvailabilityNotFoundException::new);
        if (!av.getSupervisorId().equals(userId)) {
            throw new AvailabilityAccessDeniedException();
        }
        availabilityRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Bookable slot grid: every slot in the window, with an available/taken flag. */
    @GetMapping("/{id}/slots")
    public List<SlotResponse> slots(@PathVariable Long id) {
        return availabilityService.getSlotsForAvailability(id);
    }
}
