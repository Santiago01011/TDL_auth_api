package com.ToDoList.auth.service;

import com.ToDoList.auth.dto.AuthResponse;
import com.ToDoList.auth.dto.LoginRequest;
import com.ToDoList.auth.dto.RegisterRequest;
import com.ToDoList.auth.model.PendingUser;
import com.ToDoList.auth.model.User;
import com.ToDoList.auth.repository.PendingUserRepository;
import com.ToDoList.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PendingUserRepository pendingUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;

    @Value("${application.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()) || pendingUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use.");
        }
        if (userRepository.existsByUsername(request.getUsername()) || pendingUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use.");
        }
        String verificationCode = UUID.randomUUID().toString();
        PendingUser pendingUser = PendingUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .verificationCode(verificationCode)
                .build();

        pendingUserRepository.save(pendingUser);
        log.info("Pending user saved for email: {}", request.getEmail());
        sendVerificationEmail(request.getEmail(), verificationCode);
    }

    @Transactional
    public void verify(String verificationCode) {
        PendingUser pendingUser = pendingUserRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code."));

        User user = User.builder()
                .username(pendingUser.getUsername())
                .email(pendingUser.getEmail())
                .password(pendingUser.getPasswordHash())
                .build();

        userRepository.save(user);
        log.info("User verified and created for email: {}", user.getEmail());

        pendingUserRepository.delete(pendingUser);
        log.info("Pending user deleted for email: {}", pendingUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            log.info("Authentication successful for email: {}", request.getEmail());
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for email {}: {}", request.getEmail(), e.getMessage());
            throw new IllegalArgumentException("Invalid email or password.", e);
        }

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found after successful authentication - data inconsistency?"));

        String jwtToken = jwtService.generateToken(user);
        log.info("JWT generated for email: {}", request.getEmail());

        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }

    private void sendVerificationEmail(String toEmail, String verificationCode) {
        String verificationUrl = baseUrl + "/api/auth/verify?code=" + verificationCode;
        String subject = "Verify Your Email Address";
        String text = "Please click the following link to verify your email address: " + verificationUrl;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            // TODO: Handle email sending failure, retry and retrive the link as text for manual verification
        }
    }
}
