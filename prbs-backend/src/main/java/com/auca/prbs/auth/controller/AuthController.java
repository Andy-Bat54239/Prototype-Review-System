package com.auca.prbs.auth.controller;

import com.auca.prbs.auth.dto.AuthResponse;
import com.auca.prbs.auth.dto.RefreshRequest;
import com.auca.prbs.auth.dto.SendOtpRequest;
import com.auca.prbs.auth.dto.VerifyOtpRequest;
import com.auca.prbs.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody SendOtpRequest body) {
        authService.sendOtp(body.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest body) {
        return ResponseEntity.ok(authService.verifyOtp(body.email(), body.code()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest body) {
        return ResponseEntity.ok(authService.refresh(body.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest body) {
        authService.logout(body.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
