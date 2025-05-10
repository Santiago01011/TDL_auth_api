package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.service.AuthService;
import com.TrashTDL.ServerlessAuth.exception.VerificationCodeExpiredException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class VerifyFunction {
    @FunctionName("Verify")
    public HttpResponseMessage run(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "auth/verify"
        )
        HttpRequestMessage<Void> request,
        final ExecutionContext context
    )
    {
        context.getLogger().info("Verify function triggered.");

        final String verificationCode = request.getQueryParameters().get("code");

        if (verificationCode == null || verificationCode.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please provide a valid verification code.")
                    .build();
        }

        AuthService authService = SpringContextHolder.getBean(AuthService.class);
        if (authService == null) {
            context.getLogger().severe("AuthService could not be retrieved from Spring context via SpringContextHolder.");
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initializing application services (AuthService null).")
                    .build();
        }
        try{
            String result = authService.verify(verificationCode);
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(result)
                    .build();
        } catch (VerificationCodeExpiredException e) {
            context.getLogger().warning("Verification failed: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.GONE)
                    .body(e.getMessage())
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Error during verification: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error during verification.")
                    .build();
        }
    }
    
}
