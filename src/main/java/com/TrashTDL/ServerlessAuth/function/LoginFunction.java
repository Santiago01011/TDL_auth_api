package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.dto.AuthResponse;
import com.TrashTDL.ServerlessAuth.dto.LoginRequest;
import com.TrashTDL.ServerlessAuth.service.AuthService;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.springframework.security.core.AuthenticationException;

public class LoginFunction {

    @FunctionName("Login")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "auth/login")
            HttpRequestMessage<LoginRequest> request,
            final ExecutionContext context) {

        context.getLogger().info("Login function triggered (native + SpringContextHolder).");

        AuthService authService;
        try {
            authService = SpringContextHolder.getBean(AuthService.class);
        } catch (Exception e) {
            context.getLogger().severe("Failed to get AuthService from SpringContextHolder: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initializing application services.")
                    .build();
        }

        if (authService == null) {
            context.getLogger().severe("AuthService could not be retrieved from Spring context via SpringContextHolder.");
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initializing application services (AuthService null).")
                    .build();
        }

        LoginRequest loginRequest = request.getBody();
        if (loginRequest == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a valid login request body.")
                    .build();
        }

        try {
            AuthResponse authResponse = authService.login(loginRequest);
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(authResponse)
                    .build();
        } catch (AuthenticationException ae) {
            context.getLogger().warning("Authentication failed: " + ae.getMessage());
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                    .body("Login failed: Invalid credentials.")
                    .build();
        } catch (IllegalStateException ise) {
            context.getLogger().severe("Critical error after authentication: " + ise.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Login failed: An internal error occurred.")
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Unexpected error during login: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Login failed: An unexpected error occurred.")
                    .build();
        }
    }
}
