package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.exception.VerificationCodeExpiredException;
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

import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VerifyFunction
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyFunction Tests")
class VerifyFunctionTest {

    @InjectMocks private VerifyFunction verifyFunction;
    @Mock private AuthService authService;
    @Mock private ExecutionContext context;
    @Mock private HttpRequestMessage<Void> request;
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
    @DisplayName("should return 200 OK on successful verification")
    void verify_success_returns200() {
        String validCode = "valid-code";
        when(request.getQueryParameters()).thenReturn(Map.of("code", validCode));
        when(authService.verify(validCode)).thenReturn("User verified successfully.");
        when(httpResponse.getStatus()).thenReturn(HttpStatus.OK);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = verifyFunction.run(request, context);

            assertEquals(HttpStatus.OK, response.getStatus());
            verify(responseBuilder).body("User verified successfully.");
        }
    }

    @Test
    @DisplayName("should return 410 Gone for an expired verification code")
    void verify_expiredCode_returns410() {
        String expiredCode = "expired-code";
        when(request.getQueryParameters()).thenReturn(Map.of("code", expiredCode));
        when(authService.verify(expiredCode)).thenThrow(new VerificationCodeExpiredException("Code has expired."));
        when(httpResponse.getStatus()).thenReturn(HttpStatus.GONE);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = verifyFunction.run(request, context);

            assertEquals(HttpStatus.GONE, response.getStatus());
            verify(responseBuilder).body("Code has expired.");
        }
    }

    @Test
    @DisplayName("should return 400 Bad Request when verification code is missing")
    void verify_missingCode_returns400() {
        when(request.getQueryParameters()).thenReturn(Collections.emptyMap());
        when(httpResponse.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);

        HttpResponseMessage response = verifyFunction.run(request, context);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        verify(responseBuilder).body("Please provide a valid verification code.");
    }

    @Test
    @DisplayName("should return 400 Bad Request when verification code is empty")
    void verify_emptyCode_returns400() {
        when(request.getQueryParameters()).thenReturn(Map.of("code", ""));
        when(httpResponse.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);

        HttpResponseMessage response = verifyFunction.run(request, context);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        verify(responseBuilder).body("Please provide a valid verification code.");
    }

    @Test
    @DisplayName("should return 500 Internal Server Error when AuthService is null")
    void verify_nullAuthService_returns500() {
        when(request.getQueryParameters()).thenReturn(Map.of("code", "valid-code"));
        when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(null);

            HttpResponseMessage response = verifyFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Error initializing application services (AuthService null).");
        }
    }

    @Test
    @DisplayName("should return 500 Internal Server Error on unexpected exception")
    void verify_unexpectedException_returns500() {
        String validCode = "valid-code";
        when(request.getQueryParameters()).thenReturn(Map.of("code", validCode));
        when(authService.verify(validCode)).thenThrow(new RuntimeException("Unexpected error"));
        when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(AuthService.class)).thenReturn(authService);

            HttpResponseMessage response = verifyFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Unexpected error during verification.");
        }
    }
}
