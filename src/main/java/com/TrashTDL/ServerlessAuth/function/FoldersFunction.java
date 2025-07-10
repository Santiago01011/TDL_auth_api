package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.dto.FolderResponse;
import com.TrashTDL.ServerlessAuth.model.User;
import com.TrashTDL.ServerlessAuth.repository.UserRepository;
import com.TrashTDL.ServerlessAuth.service.DBHandler;
import com.TrashTDL.ServerlessAuth.service.JwtService;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FoldersFunction {

    @FunctionName("GetFolders")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS, // Auth is handled manually via JWT
                    route = "v2/folders")
            HttpRequestMessage<Void> request,
            final ExecutionContext context) {

        context.getLogger().info("GetFolders function triggered.");

        String authHeader = request.getHeaders().get("authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            context.getLogger().warning("Missing or invalid Authorization header.");
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED)
                    .body("Authorization header with Bearer token is required.")
                    .build();
        }
        String token = authHeader.substring(7);

        try {
            JwtService jwtService = SpringContextHolder.getBean(JwtService.class);
            UserRepository userRepository = SpringContextHolder.getBean(UserRepository.class);
            DBHandler dbHandler = SpringContextHolder.getBean(DBHandler.class);

            // Validate token and user
            final UUID userId = jwtService.extractUserId(token);
            final String userEmail = jwtService.extractUsername(token);
            final Optional<User> userOptional = userRepository.findByEmail(userEmail);

            if (userOptional.isEmpty() || !jwtService.isTokenValid(token, userOptional.get())) {
                context.getLogger().warning("Token validation failed for user: " + userEmail);
                return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.").build();
            }

            // Fetch Folders
            List<FolderResponse> folders = dbHandler.getFoldersForUser(userId);
            context.getLogger().info("Successfully retrieved " + folders.size() + " folders for user " + userId);

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(folders)
                    .build();

        } catch (SQLException e) {
            context.getLogger().severe("Database error while fetching folders: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve folders due to a database error.")
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Unexpected error while fetching folders: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.")
                    .build();
        }
    }
}