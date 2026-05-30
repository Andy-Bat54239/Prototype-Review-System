package com.auca.prbs.user.controller;

import com.auca.prbs.user.dto.ImportUsersResponse;
import com.auca.prbs.user.dto.UpdateUserStatusRequest;
import com.auca.prbs.user.dto.UserResponse;
import com.auca.prbs.user.entity.User;
import com.auca.prbs.user.exception.UserNotFoundException;
import com.auca.prbs.user.repository.UserRepository;
import com.auca.prbs.user.service.UserImportService;
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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserImportService userImportService;

    /** Admin: list all users. */
    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(UserResponse::of).toList());
    }

    /** Admin: flip a user's ACTIVE / INACTIVE flag. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUserStatusRequest body) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setStatus(body.status());
        return ResponseEntity.ok(UserResponse.of(userRepository.save(user)));
    }

    /** Admin: bulk-onboard from a registrar CSV. */
    @PostMapping("/import")
    public ResponseEntity<ImportUsersResponse> importCsv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userImportService.importCsv(file));
    }
}
