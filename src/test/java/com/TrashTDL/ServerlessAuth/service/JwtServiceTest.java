package com.TrashTDL.ServerlessAuth.service;

import com.TrashTDL.ServerlessAuth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    
    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set required properties via reflection
        ReflectionTestUtils.setField(jwtService, "secretKeyString", "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }
    
    @Test
    void testExtractUserId() {
        // Create a mock user
        UUID expectedUserId = UUID.randomUUID();
        User user = User.builder()
                .userId(expectedUserId)
                .email("test@example.com")
                .username("testuser")
                .password("password")
                .build();
        
        // Generate token
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        
        // Extract userId
        UUID extractedUserId = jwtService.extractUserId(token);
        assertEquals(expectedUserId, extractedUserId);
    }
    
    @Test
    void testExtractUserIdFromInvalidToken() {
        assertThrows(Exception.class, () -> {
            jwtService.extractUserId("invalid.token.here");
        });
    }
}