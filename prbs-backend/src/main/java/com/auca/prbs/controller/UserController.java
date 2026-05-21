package com.auca.prbs.controller;

import com.auca.prbs.dto.ImportUsersResponse;
import com.auca.prbs.dto.UpdateUserStatusRequest;
import com.auca.prbs.dto.UserResponse;
import com.auca.prbs.entity.User;
import com.auca.prbs.exception.UserNotFoundException;
import com.auca.prbs.repository.UserRepository;
import com.auca.prbs.service.UserImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin user management. SecurityConfig already restricts {@code /api/v1/users/**}
 * to the ADMIN role; no further checks needed here.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserImportService userImportService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(UserResponse::of)
                .toList());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUserStatusRequest body) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setStatus(body.status());
        return ResponseEntity.ok(UserResponse.of(userRepository.save(user)));
    }

    /**
     * Bulk-onboard users from a registrar CSV. See {@link UserImportService}
     * for the expected format. Always returns 200 with a per-row summary —
     * even if some rows failed — so the admin UI can show partial-success.
     */
    @PostMapping("/import")
    public ResponseEntity<ImportUsersResponse> importCsv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userImportService.importCsv(file));
    }
}
