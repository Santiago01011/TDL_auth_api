package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.dto.RegisterRequest;
import com.TrashTDL.ServerlessAuth.service.AuthService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegisterFunction
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterFunction Tests")
class RegisterFunctionTest {

    @InjectMocks private RegisterFunction registerFunction;
    @Mock private AuthService authService;
    @Mock private ExecutionContext context;
    @Mock private HttpRequestMessage<RegisterRequest> request;
    @Mock private HttpResponseMessage.Builder responseBuilder;
    @Mock private HttpResponseMessage httpResponse;

    @BeforeEach
    void setUp() {
        when(context.getLogger()).thenReturn(Logger.getLogger("test-logger"));
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(responseBuilder);
        when(responseBuilder.body(any())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(httpResponse);
    }

    @Test
    @DisplayName("should return 200 OK with verification code on successful registration")
    void register_success_returns200() {
        RegisterRequest registerRequest = new RegisterRequest("newuser", "new@email.com", "password");
        String verificationCode = "123456";
        when(request.getBody()).thenReturn(registerRequest);
        when(authService.register(registerRequest)).thenReturn(verificationCode);
        when(httpResponse.getStatus()).thenReturn(HttpStatus.OK);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = registerFunction.run(request, context);

            assertEquals(HttpStatus.OK, response.getStatus());
            verify(responseBuilder).body("Registration successful. Verification code: " + verificationCode);
        }
    }

    @Test
    @DisplayName("should return 400 Bad Request on IllegalArgumentException (e.g., user exists)")
    void register_illegalArgument_returns400() {
        RegisterRequest registerRequest = new RegisterRequest("existinguser", "exist@email.com", "password");
        when(request.getBody()).thenReturn(registerRequest);
        when(authService.register(registerRequest)).thenThrow(new IllegalArgumentException("User already exists"));
        when(httpResponse.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = registerFunction.run(request, context);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
            verify(responseBuilder).body("Registration failed: User already exists");
        }
    }

    @Test
    @DisplayName("should return 400 Bad Request for null request body")
    void register_nullBody_returns400() {
        when(request.getBody()).thenReturn(null);
        when(httpResponse.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);

        HttpResponseMessage response = registerFunction.run(request, context);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        verify(responseBuilder).body("Please pass a valid register request body.");
    }

    @Test
    @DisplayName("should return 500 Internal Server Error when AuthService is null")
    void register_nullAuthService_returns500() {
        RegisterRequest registerRequest = new RegisterRequest("user", "user@email.com", "password");
        when(request.getBody()).thenReturn(registerRequest);
        when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(null);

            HttpResponseMessage response = registerFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Error initializing application services (AuthService null).");
        }
    }

    @Test
    @DisplayName("should return 500 Internal Server Error on unexpected exception")
    void register_unexpectedException_returns500() {
        RegisterRequest registerRequest = new RegisterRequest("user", "user@email.com", "password");
        when(request.getBody()).thenReturn(registerRequest);
        when(authService.register(registerRequest)).thenThrow(new RuntimeException("Unexpected error"));
        when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = registerFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Unexpected error during registration.");
        }
    }
}
