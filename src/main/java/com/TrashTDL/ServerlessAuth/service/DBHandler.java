package com.TrashTDL.ServerlessAuth.service;

import com.TrashTDL.ServerlessAuth.dto.Command;
import com.TrashTDL.ServerlessAuth.dto.FolderResponse;
import com.TrashTDL.ServerlessAuth.dto.SyncResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DBHandler {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final ObjectMapper objectMapper;
    
    public DBHandler() {
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper to ignore unknown properties
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public SyncResponse syncCommands(UUID userId, List<Command> commands) throws JsonProcessingException, SQLException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (commands == null || commands.isEmpty()) {
            log.warn("No commands provided for sync for user {}", userId);
            return SyncResponse.builder()
                    .success(List.of())
                    .conflicts(List.of())
                    .failed(List.of())
                    .build();
        }
        
        log.info("Syncing {} commands for user {}", commands.size(), userId);
        
        try {
            // Convert commands list to JSON string for the database function
            // The PostgreSQL function expects {"commands": [...]} format
            String commandsJson = objectMapper.writeValueAsString(Map.of("commands", commands));
            log.info("Commands JSON for user {}: {}", userId, commandsJson);
            
            // Call the PostgreSQL function todo.merge_task_commands
            String sql = "SELECT todo.merge_task_commands(?, ?::jsonb)";
            
            String resultJson = jdbcTemplate.queryForObject(sql, String.class, userId, commandsJson);
            
            log.info("Database function result for user {}: {}", userId, resultJson);
            
            if (resultJson == null || resultJson.trim().isEmpty()) {
                throw new SQLException("Database function returned null or empty result");
            }
            
            // Parse the result JSON back to SyncResponse
            SyncResponse response = objectMapper.readValue(resultJson, SyncResponse.class);
            
            log.info("Sync completed for user {}: {} success, {} conflicts, {} failed", 
                    userId, 
                    response.getSuccess() != null ? response.getSuccess().size() : 0,
                    response.getConflicts() != null ? response.getConflicts().size() : 0,
                    response.getFailed() != null ? response.getFailed().size() : 0);
            
            return response;
        } catch (JsonProcessingException e) {
            log.error("JSON processing error for user {}: {}", userId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Database error executing sync for user {}: {}", userId, e.getMessage(), e);
            throw new SQLException("Database sync operation failed: " + e.getMessage(), e);
        }
    }

    public List<FolderResponse> getFoldersForUser(UUID userId) throws SQLException {
        String sql = "SELECT folder_id, folder_name FROM todo.get_accessible_folders(?)";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new FolderResponse(
                    rs.getObject("folder_id", UUID.class),
                    rs.getString("folder_name")
            ), userId);
        } catch (Exception e) {
            log.error("Database error fetching folders for user {}: {}", userId, e.getMessage(), e);
            throw new SQLException("Failed to retrieve folders for user: " + userId, e);
        }
    }
}