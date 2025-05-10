package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.dto.RegisterRequest;
import com.TrashTDL.ServerlessAuth.service.AuthService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class RegisterFunction {
    @FunctionName("Register")
    public HttpResponseMessage run (
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "auth/register"
        )
        HttpRequestMessage<RegisterRequest> request,
        final ExecutionContext context
    ){
        context.getLogger().info("Register function triggered (native + SpringContextHolder).");

        RegisterRequest registerRequest = request.getBody();
        if (registerRequest == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a valid register request body.")
                    .build();
        }

        AuthService authService = SpringContextHolder.getBean(AuthService.class);
        if (authService == null) {
            context.getLogger().severe("AuthService could not be retrieved from Spring context via SpringContextHolder.");
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initializing application services (AuthService null).")
                    .build();
        }

        try {
            String verificationCode = authService.register(registerRequest);
            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Registration successful. Verification code: " + verificationCode)
                    .build();
        } catch (IllegalArgumentException e) {
            context.getLogger().warning("Registration failed: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Registration failed: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Unexpected error during registration: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error during registration.")
                    .build();
        }
        

    }
}
