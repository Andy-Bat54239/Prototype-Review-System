package com.auca.prbs.controller;

import com.auca.prbs.dto.BookingResponse;
import com.auca.prbs.dto.CreateBookingRequest;
import com.auca.prbs.dto.UpdateBookingStatusRequest;
import com.auca.prbs.entity.Booking;
import com.auca.prbs.entity.User;
import com.auca.prbs.entity.UserRole;
import com.auca.prbs.exception.UserNotFoundException;
import com.auca.prbs.repository.BookingRepository;
import com.auca.prbs.repository.UserRepository;
import com.auca.prbs.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /** Student creates a booking against a chosen Availability slot. */
    @PostMapping
    public ResponseEntity<BookingResponse> create(Authentication auth,
                                                  @Valid @RequestBody CreateBookingRequest body) {
        Long userId = (Long) auth.getPrincipal();
        Booking booking = bookingService.createBookingForStudent(userId, body);
        return ResponseEntity.ok(toResponse(booking));
    }

    /**
     * Bookings owned by the authenticated user. Role determines which side:
     * STUDENT → their own; SUPERVISOR → ones they supervise; ADMIN → empty
     * (admin should use a dedicated admin list once it exists).
     */
    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> mine(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        List<Booking> bookings = switch (user.getRole()) {
            case STUDENT    -> bookingRepository.findByStudentIdOrderBySlotAtDesc(userId);
            case SUPERVISOR -> bookingRepository.findBySupervisorIdOrderBySlotAtDesc(userId);
            case ADMIN      -> List.of();
        };
        return ResponseEntity.ok(bookings.stream().map(this::toResponse).toList());
    }

    /** Either the booking's student or its supervisor can cancel. */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        Booking booking = bookingService.cancelBookingByUser(id, userId);
        return ResponseEntity.ok(toResponse(booking));
    }

    /** Supervisor marks the session COMPLETED or NO_SHOW after the fact. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(Authentication auth,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody UpdateBookingStatusRequest body) {
        Long userId = (Long) auth.getPrincipal();
        Booking booking = bookingService.updateStatus(id, userId, body.status());
        return ResponseEntity.ok(toResponse(booking));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /** Hydrates the response with student + supervisor names in one user-cache map. */
    private BookingResponse toResponse(Booking booking) {
        Map<Long, User> cache = new HashMap<>();
        User student = cache.computeIfAbsent(booking.getStudentId(),
                id -> userRepository.findById(id).orElse(null));
        User supervisor = cache.computeIfAbsent(booking.getSupervisorId(),
                id -> userRepository.findById(id).orElse(null));
        return BookingResponse.of(booking, student, supervisor);
    }
}
