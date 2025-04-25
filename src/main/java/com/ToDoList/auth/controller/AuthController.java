package com.ToDoList.auth.controller;

import com.ToDoList.auth.dto.AuthResponse;
import com.ToDoList.auth.dto.LoginRequest;
import com.ToDoList.auth.dto.RegisterRequest;
import com.ToDoList.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
        try {
            String verification_code = authService.register(request);
            log.info("Registration request successful for email: {}. If you did not receive an email, manually go to the verification link: {}", request.getEmail(), baseUrl + "/api/auth/verify?code=" + verification_code);
            return ResponseEntity.ok("Registration successful. Please check your email for verification link. If you did not receive an email, manually go to the verification link: " + baseUrl + "/api/auth/verify?code=" + verification_code);
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during registration for email {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during registration.");
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam("code") String code) {
        try {
            authService.verify(code);
            log.info("Verification successful for code: {}", code);
            return ResponseEntity.ok("Account verified successfully!");
        } catch (IllegalArgumentException e) {
            log.warn("Verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during verification for code {}: {}", code, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during verification.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            log.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during login for email {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during login.");
        }
    }
}
