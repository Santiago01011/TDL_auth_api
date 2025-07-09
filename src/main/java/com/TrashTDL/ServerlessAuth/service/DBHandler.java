package com.TrashTDL.ServerlessAuth.service;

import com.TrashTDL.ServerlessAuth.dto.Command;
import com.TrashTDL.ServerlessAuth.dto.SyncResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DBHandler {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SyncResponse syncCommands(UUID userId, List<Command> commands) throws JsonProcessingException, SQLException {
        log.info("Syncing {} commands for user {}", commands.size(), userId);
        
        // Convert commands list to JSON string for the database function
        String commandsJson = objectMapper.writeValueAsString(commands);
        
        // Call the PostgreSQL function todo.merge_task_commands
        String sql = "SELECT todo.merge_task_commands(?, ?::jsonb)";
        
        try {
            String resultJson = jdbcTemplate.queryForObject(sql, String.class, userId, commandsJson);
            
            // Parse the result JSON back to SyncResponse
            SyncResponse response = objectMapper.readValue(resultJson, SyncResponse.class);
            
            log.info("Sync completed for user {}: {} success, {} conflicts, {} failed", 
                    userId, 
                    response.getSuccess() != null ? response.getSuccess().size() : 0,
                    response.getConflicts() != null ? response.getConflicts().size() : 0,
                    response.getFailed() != null ? response.getFailed().size() : 0);
            
            return response;
        } catch (Exception e) {
            log.error("Error executing sync for user {}: {}", userId, e.getMessage(), e);
            throw new SQLException("Database sync operation failed", e);
        }
    }
}