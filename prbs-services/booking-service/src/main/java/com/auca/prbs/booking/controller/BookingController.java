package com.auca.prbs.booking.controller;

import com.auca.prbs.booking.client.UserServiceClient;
import com.auca.prbs.booking.client.UserView;
import com.auca.prbs.booking.dto.BookingResponse;
import com.auca.prbs.booking.dto.CreateBookingRequest;
import com.auca.prbs.booking.dto.UpdateBookingStatusRequest;
import com.auca.prbs.booking.entity.Booking;
import com.auca.prbs.booking.exception.BookingExceptions.UserNotFoundException;
import com.auca.prbs.booking.repository.BookingRepository;
import com.auca.prbs.booking.service.BookingService;
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
    private final UserServiceClient userServiceClient;

    @PostMapping
    public ResponseEntity<BookingResponse> create(Authentication auth,
                                                  @Valid @RequestBody CreateBookingRequest body) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(toResponse(bookingService.createBookingForStudent(userId, body)));
    }

    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> mine(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        UserView user = userServiceClient.findById(userId).orElseThrow(UserNotFoundException::new);

        List<Booking> bookings = switch (user.role()) {
            case "STUDENT"    -> bookingRepository.findByStudentIdOrderBySlotAtDesc(userId);
            case "SUPERVISOR" -> bookingRepository.findBySupervisorIdOrderBySlotAtDesc(userId);
            default           -> List.of();   // ADMIN gets the dedicated admin list once it exists
        };
        return ResponseEntity.ok(bookings.stream().map(this::toResponse).toList());
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(toResponse(bookingService.cancelBookingByUser(id, userId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(Authentication auth, @PathVariable Long id,
                                                        @Valid @RequestBody UpdateBookingStatusRequest body) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(toResponse(bookingService.updateStatus(id, userId, body.status())));
    }

    private BookingResponse toResponse(Booking booking) {
        Map<Long, UserView> cache = new HashMap<>();
        UserView student    = cache.computeIfAbsent(booking.getStudentId(),
                id -> userServiceClient.findById(id).orElse(null));
        UserView supervisor = cache.computeIfAbsent(booking.getSupervisorId(),
                id -> userServiceClient.findById(id).orElse(null));
        return BookingResponse.of(booking, student, supervisor);
    }
}
