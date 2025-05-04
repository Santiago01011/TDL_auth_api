package com.ToDoList.auth.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;

import com.ToDoList.auth.dto.RegisterRequest;
import com.ToDoList.auth.config.ApplicationContextProvider;
import com.ToDoList.auth.service.AuthService;

public class RegisterFunction {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AuthService authService;

    public RegisterFunction() {
        this.authService = ApplicationContextProvider.getBean(AuthService.class);
    }

    @FunctionName("register")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS, route = "auth/register")
        HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context
    ) {
        try {
            String body = request.getBody().orElseThrow(() -> new IllegalArgumentException("Missing request body"));
            RegisterRequest register = MAPPER.readValue(body, RegisterRequest.class);
            String code = authService.register(register);
            String link = "https://" + System.getenv("FUNCTIONS_HOSTNAME") + "/api/auth/verify?code=" + code;
            String message = "Registration successful. Verify via: " + link;
            return request.createResponseBuilder(HttpStatus.OK)
                          .header("Content-Type", "text/plain")
                          .body(message)
                          .build();
        } catch (IllegalArgumentException | IOException ex) {
            context.getLogger().severe("Register failed: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                          .body(ex.getMessage())
                          .build();
        } catch (Exception ex) {
            context.getLogger().severe("Internal error: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("An error occurred processing the registration request.")
                          .build();
        }
    }
}