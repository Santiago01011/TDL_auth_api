package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.dto.Command;
import com.TrashTDL.ServerlessAuth.dto.SyncRequest;
import com.TrashTDL.ServerlessAuth.dto.SyncResponse;
import com.TrashTDL.ServerlessAuth.model.User;
import com.TrashTDL.ServerlessAuth.repository.UserRepository;
import com.TrashTDL.ServerlessAuth.service.DBHandler;
import com.TrashTDL.ServerlessAuth.service.JwtService;
import com.TrashTDL.ServerlessAuth.service.SyncValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Refactored and organized tests for SyncFunction.
 * Uses nested classes to group tests by concern (Auth, Validation, Processing).
 * Reduces boilerplate by using helper methods for common mock setups.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SyncFunction Tests")
class SyncFunctionTest {

    // Mocks for all required services
    @Mock private JwtService jwtService;
    @Mock private DBHandler dbHandler;
    @Mock private UserRepository userRepository;
    @Mock private SyncValidationService validationService;
    @Mock private ExecutionContext context;
    @Mock private HttpRequestMessage<SyncRequest> request;
    @Mock private HttpResponseMessage.Builder responseBuilder;
    @Mock private HttpResponseMessage httpResponse;

    private SyncFunction syncFunction;
    private final UUID testUserId = UUID.randomUUID();
    private final String testUserEmail = "test@example.com";
    private final String testToken = "valid-jwt-token";

    @BeforeEach
    void setUp() {
        // Instantiate the function under test
        syncFunction = new SyncFunction();

        // Mock the logger to avoid NullPointerExceptions
        when(context.getLogger()).thenReturn(Logger.getLogger("test-logger"));
        
        // Common mock for the response builder chain
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(responseBuilder);
        when(responseBuilder.body(any())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(httpResponse);
    }

    /**
     * Helper method to mock the SpringContextHolder to return our mocked service instances.
     * @param springContextMock The static mock instance.
     */
    private void setupMockServices(MockedStatic<SpringContextHolder> springContextMock) {
        springContextMock.when(() -> SpringContextHolder.getBean(JwtService.class)).thenReturn(jwtService);
        springContextMock.when(() -> SpringContextHolder.getBean(DBHandler.class)).thenReturn(dbHandler);
        springContextMock.when(() -> SpringContextHolder.getBean(UserRepository.class)).thenReturn(userRepository);
        springContextMock.when(() -> SpringContextHolder.getBean(SyncValidationService.class)).thenReturn(validationService);
    }

    /**
     * Helper method to set up a valid, authenticated user scenario.
     */
    private void setupValidUserAuthentication() {
        User mockUser = mock(User.class);
        when(request.getHeaders()).thenReturn(Map.of("authorization", "Bearer " + testToken));
        when(jwtService.extractUsername(testToken)).thenReturn(testUserEmail);
        when(jwtService.extractUserId(testToken)).thenReturn(testUserId);
        when(userRepository.findByEmail(testUserEmail)).thenReturn(Optional.of(mockUser));
        when(jwtService.isTokenValid(testToken, mockUser)).thenReturn(true);
    }

    @Nested
    @DisplayName("Authentication and Authorization Tests")
    class AuthTests {
        @Test
        @DisplayName("should return 401 Unauthorized when Authorization header is missing")
        void syncFunction_missingAuthHeader_returns401() {
            when(request.getHeaders()).thenReturn(Collections.emptyMap());
            when(httpResponse.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED);

            HttpResponseMessage response = syncFunction.run(request, context);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
            verify(responseBuilder).body("Authorization header with Bearer token is required");
        }

        @Test
        @DisplayName("should return 401 Unauthorized when token is invalid")
        void syncFunction_invalidToken_returns401() {
            when(request.getHeaders()).thenReturn(Map.of("authorization", "Bearer " + testToken));
            when(jwtService.extractUsername(testToken)).thenThrow(new RuntimeException("Invalid Token"));
            when(httpResponse.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED);
            
            try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
                setupMockServices(springMock);
                HttpResponseMessage response = syncFunction.run(request, context);
                assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
            }
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class ValidationTests {
        @Test
        @DisplayName("should return 400 Bad Request for null request body")
        void syncFunction_nullBody_returns400() {
            try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
                setupMockServices(springMock);
                setupValidUserAuthentication();
                when(request.getBody()).thenReturn(null);
                when(httpResponse.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);

                HttpResponseMessage response = syncFunction.run(request, context);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
                verify(responseBuilder).body("Request body must contain a 'commands' array");
            }
        }

        @Test
        @DisplayName("should return 400 Bad Request when command validation fails")
        void syncFunction_commandValidationFails_returns400() {
            SyncRequest syncRequest = new SyncRequest();
            syncRequest.setCommands(Collections.singletonList(new Command()));
            List<String> errors = List.of("Invalid command action");

            try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
                setupMockServices(springMock);
                setupValidUserAuthentication();
                when(request.getBody()).thenReturn(syncRequest);
                when(validationService.validateCommands(any())).thenReturn(errors);
                when(httpResponse.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);

                HttpResponseMessage response = syncFunction.run(request, context);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
                verify(responseBuilder).body("Validation errors: Invalid command action");
            }
        }
    }

    @Nested
    @DisplayName("Sync Processing and Error Handling Tests")
    class ProcessingTests {
        private SyncRequest validSyncRequest;

        @BeforeEach
        void processingSetup() {
            validSyncRequest = new SyncRequest();
            validSyncRequest.setCommands(Collections.singletonList(new Command()));
        }

        @Test
        @DisplayName("should return 200 OK for a successful sync")
        void syncFunction_successfulSync_returns200() throws Exception {
            SyncResponse syncResponse = new SyncResponse();

            try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
                setupMockServices(springMock);
                setupValidUserAuthentication();
                when(request.getBody()).thenReturn(validSyncRequest);
                when(validationService.validateCommands(any())).thenReturn(Collections.emptyList());
                when(dbHandler.syncCommands(testUserId, validSyncRequest.getCommands())).thenReturn(syncResponse);
                when(httpResponse.getStatus()).thenReturn(HttpStatus.OK);

                HttpResponseMessage response = syncFunction.run(request, context);

                assertEquals(HttpStatus.OK, response.getStatus());
                verify(dbHandler).syncCommands(testUserId, validSyncRequest.getCommands());
                verify(responseBuilder).body(syncResponse);
            }
        }

        @Test
        @DisplayName("should return 500 Internal Server Error on SQLException")
        void syncFunction_sqlException_returns500() throws Exception {
            try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
                setupMockServices(springMock);
                setupValidUserAuthentication();
                when(request.getBody()).thenReturn(validSyncRequest);
                when(validationService.validateCommands(any())).thenReturn(Collections.emptyList());
                when(dbHandler.syncCommands(any(), any())).thenThrow(new SQLException("DB connection failed"));
                when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

                HttpResponseMessage response = syncFunction.run(request, context);

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
                verify(responseBuilder).body("Database operation failed");
            }
        }
        
        @Test
        @DisplayName("should return 400 Bad Request on JsonProcessingException")
        void syncFunction_jsonException_returns400() throws Exception {
            try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
                setupMockServices(springMock);
                setupValidUserAuthentication();
                when(request.getBody()).thenReturn(validSyncRequest);
                when(validationService.validateCommands(any())).thenReturn(Collections.emptyList());
                when(dbHandler.syncCommands(any(), any())).thenThrow(new JsonProcessingException("Invalid JSON") {});
                when(httpResponse.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);

                HttpResponseMessage response = syncFunction.run(request, context);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
                verify(responseBuilder).body("Invalid JSON in request data");
            }
        }
    }
}
