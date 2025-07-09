package com.TrashTDL.ServerlessAuth.service;

import com.TrashTDL.ServerlessAuth.dto.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyncValidationServiceTest {

    private SyncValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new SyncValidationService();
    }

    @Test
    void testValidCommands() {
        Command validCommand = Command.builder()
                .action("create")
                .entityType("task")
                .entityId("123")
                .data("some data")
                .clientTimestamp("2023-01-01T00:00:00Z")
                .build();

        List<String> errors = validationService.validateCommands(Arrays.asList(validCommand));
        assertTrue(errors.isEmpty());
    }

    @Test
    void testInvalidAction() {
        Command invalidCommand = Command.builder()
                .action("invalid_action")
                .entityType("task")
                .entityId("123")
                .build();

        List<String> errors = validationService.validateCommands(Arrays.asList(invalidCommand));
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Invalid action"));
    }

    @Test
    void testMissingRequiredFields() {
        Command invalidCommand = Command.builder()
                .action("")
                .entityType("")
                .entityId("")
                .build();

        List<String> errors = validationService.validateCommands(Arrays.asList(invalidCommand));
        
        // Debug: print the errors to understand what's happening
        System.out.println("Validation errors: " + errors);
        
        assertEquals(3, errors.size()); // Back to 3 errors since we fixed the validation logic
        assertTrue(errors.stream().anyMatch(error -> error.contains("Action is required")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("Entity type is required")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("Entity ID is required")));
    }

    @Test
    void testNullCommands() {
        List<String> errors = validationService.validateCommands(null);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Commands list cannot be null"));
    }
}