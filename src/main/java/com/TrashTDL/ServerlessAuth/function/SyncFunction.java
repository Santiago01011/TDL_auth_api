package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.dto.SyncRequest;
import com.TrashTDL.ServerlessAuth.dto.SyncResponse;
import com.TrashTDL.ServerlessAuth.service.DBHandler;
import com.TrashTDL.ServerlessAuth.service.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.sql.SQLException;
import java.util.UUID;

public class SyncFunction {

    @FunctionName("SyncCommands")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "api/v2/sync/commands")
            HttpRequestMessage<SyncRequest> request,
            final ExecutionContext context) {

        context.getLogger().info("SyncCommands function triggered.");

        // Extract JWT token from Authorization header
        String authHeader = request.getHeaders().get("authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            context.getLogger().warning("Missing or invalid Authorization header");
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                    .body("Authorization header with Bearer token is required")
                    .build();
        }

        String token = authHeader.substring(7);

        // Get Spring beans
        JwtService jwtService;
        DBHandler dbHandler;
        try {
            jwtService = SpringContextHolder.getBean(JwtService.class);
            dbHandler = SpringContextHolder.getBean(DBHandler.class);
        } catch (Exception e) {
            context.getLogger().severe("Failed to get Spring beans: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initializing application services")
                    .build();
        }

        if (jwtService == null || dbHandler == null) {
            context.getLogger().severe("Required services could not be retrieved from Spring context");
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initializing application services")
                    .build();
        }

        // Extract and validate user ID from JWT
        UUID userId;
        try {
            userId = jwtService.extractUserId(token);
            if (userId == null) {
                context.getLogger().warning("Invalid JWT token: userId not found");
                return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or expired token")
                        .build();
            }
        } catch (Exception e) {
            context.getLogger().warning("Error parsing JWT token: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token")
                    .build();
        }

        // Validate request body
        SyncRequest syncRequest = request.getBody();
        if (syncRequest == null || syncRequest.getCommands() == null) {
            context.getLogger().warning("Invalid request body: missing commands");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Request body must contain a 'commands' array")
                    .build();
        }

        // Process sync commands
        try {
            SyncResponse response = dbHandler.syncCommands(userId, syncRequest.getCommands());
            
            context.getLogger().info("Sync successful for user " + userId + " with " + syncRequest.getCommands().size() + " commands");
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .body(response)
                    .build();
                    
        } catch (JsonProcessingException e) {
            context.getLogger().severe("JSON processing error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON in request data")
                    .build();
        } catch (SQLException e) {
            context.getLogger().severe("Database error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database operation failed")
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Unexpected error during sync: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during sync")
                    .build();
        }
    }
}