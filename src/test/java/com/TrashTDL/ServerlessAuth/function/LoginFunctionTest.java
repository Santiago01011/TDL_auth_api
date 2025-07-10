package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.dto.AuthResponse;
import com.TrashTDL.ServerlessAuth.dto.LoginRequest;
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
import org.springframework.security.core.AuthenticationException;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginFunction
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginFunction Tests")
class LoginFunctionTest {

    @InjectMocks private LoginFunction loginFunction;
    @Mock private AuthService authService;
    @Mock private ExecutionContext context;
    @Mock private HttpRequestMessage<LoginRequest> request;
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
    @DisplayName("should return 200 OK with AuthResponse on successful login")
    void login_success_returns200() {
        LoginRequest loginRequest = new LoginRequest("user", "pass", "");
        AuthResponse authResponse = new AuthResponse("token", UUID.randomUUID());
        when(request.getBody()).thenReturn(loginRequest);
        when(authService.login(loginRequest)).thenReturn(authResponse);
        when(httpResponse.getStatus()).thenReturn(HttpStatus.OK);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);
            
            HttpResponseMessage response = loginFunction.run(request, context);

            assertEquals(HttpStatus.OK, response.getStatus());
            verify(responseBuilder).body(authResponse);
        }
    }

    @Test
    @DisplayName("should return 401 Unauthorized on AuthenticationException")
    void login_authException_returns401() {
        LoginRequest loginRequest = new LoginRequest("user", "wrongpass", "");
        when(request.getBody()).thenReturn(loginRequest);
        when(authService.login(loginRequest)).thenThrow(new AuthenticationException("Bad credentials") {});
        when(httpResponse.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = loginFunction.run(request, context);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
            verify(responseBuilder).body("Login failed: Invalid credentials.");
        }
    }

    @Test
    @DisplayName("should return 400 Bad Request for null request body")
    void login_nullBody_returns400() {
        when(request.getBody()).thenReturn(null);
        when(httpResponse.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
             springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);
            
            HttpResponseMessage response = loginFunction.run(request, context);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
            verify(responseBuilder).body("Please pass a valid login request body.");
        }
    }

   @Test
    @DisplayName("should return 500 Internal Server Error when AuthService is null")
    void login_nullAuthService_returns500() {
        // NOTE: We do NOT mock request.getBody() here, because the function will
        // return an error before it ever tries to read the body.
        when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            // Simulate the Spring context failing to provide the bean
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(null);

            HttpResponseMessage response = loginFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Error initializing application services (AuthService null).");
        }
    }

    @Test
    @DisplayName("should return 500 Internal Server Error when AuthService fails to initialize")
    void login_serviceInitializationFailure_returns500() {
        when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            // Simulate an exception during bean retrieval
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class))
                        .thenThrow(new RuntimeException("Service initialization failed"));

            HttpResponseMessage response = loginFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Error initializing application services.");
        }
    }

    @Test
    @DisplayName("should return 500 Internal Server Error on IllegalStateException")
    void login_illegalStateException_returns500() {
        LoginRequest loginRequest = new LoginRequest("user", "pass", "");
        when(request.getBody()).thenReturn(loginRequest);
        when(authService.login(loginRequest)).thenThrow(new IllegalStateException("Critical error"));
        when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = loginFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Login failed: An internal error occurred.");
        }
    }

    @Test
    @DisplayName("should return 500 Internal Server Error on unexpected exception")
    void login_unexpectedException_returns500() {
        LoginRequest loginRequest = new LoginRequest("user", "pass", "");
        when(request.getBody()).thenReturn(loginRequest);
        when(authService.login(loginRequest)).thenThrow(new RuntimeException("Unexpected error"));
        when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = loginFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Login failed: An unexpected error occurred.");
        }
    }
}
