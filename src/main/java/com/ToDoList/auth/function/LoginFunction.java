package com.ToDoList.auth.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;

import com.ToDoList.auth.dto.LoginRequest;
import com.ToDoList.auth.dto.AuthResponse;
import com.ToDoList.auth.service.AuthService;
import com.ToDoList.auth.config.ApplicationContextProvider;

public class LoginFunction {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AuthService authService;

    public LoginFunction() {
        this.authService = ApplicationContextProvider.getBean(AuthService.class);
    }

    @FunctionName("login")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS, route = "auth/login")
        HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context
    ) {
        try {
            String body = request.getBody().orElseThrow(() -> new IllegalArgumentException("Missing request body"));
            LoginRequest login = MAPPER.readValue(body, LoginRequest.class);
            AuthResponse response = authService.login(login);
            String json = MAPPER.writeValueAsString(response);
            return request.createResponseBuilder(HttpStatus.OK)
                          .header("Content-Type", "application/json")
                          .body(json)
                          .build();
        } catch (IllegalArgumentException | IOException ex) {
            context.getLogger().severe("Login failed: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                          .body(ex.getMessage())
                          .build();
        } catch (Exception ex) {
            context.getLogger().severe("Internal error: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("An error occurred processing the login request.")
                          .build();
        }
    }
}