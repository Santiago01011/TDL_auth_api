package com.ToDoList.auth.controller;

import com.ToDoList.auth.dto.AuthResponse;
import com.ToDoList.auth.dto.LoginRequest;
import com.ToDoList.auth.dto.RegisterRequest;
import com.ToDoList.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    @Value("${application.base-url}")
    private String baseUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String verification_code = authService.register(request);
        log.info("Registration request successful for email: {}. If you did not receive an email, manually go to the verification link: {}", request.getEmail(), baseUrl + "/api/auth/verify?code=" + verification_code);
        return ResponseEntity.ok("Registration successful. Please check your email for verification link. If you did not receive an email, manually go to the verification link: " + baseUrl + "/api/auth/verify?code=" + verification_code);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam("code") String code) {
        authService.verify(code);
        log.info("Verification successful for code: {}", code);
        return ResponseEntity.ok("Account verified successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        log.info("Login successful for identifier: {}", request.getEmail() != null ? request.getEmail() : request.getUsername());
        return ResponseEntity.ok(response);
    }
}
