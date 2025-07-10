package com.TrashTDL.ServerlessAuth.service;

import com.TrashTDL.ServerlessAuth.dto.Command;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

@Component
public class SyncValidationService {

    public List<String> validateCommands(List<Command> commands) {
        List<String> errors = new ArrayList<>();
        
        if (commands == null) {
            errors.add("Commands list cannot be null");
            return errors;
        }
        
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            String prefix = "Command " + (i + 1) + ": ";
            
            if (command == null) {
                errors.add(prefix + "Command cannot be null");
                continue;
            }
            
            // Check for action field (legacy) or type field (new format)
            String actionOrType = command.getAction();
            if (actionOrType == null || actionOrType.trim().isEmpty()) {
                actionOrType = command.getType();
            }
            
            if (actionOrType == null || actionOrType.trim().isEmpty()) {
                errors.add(prefix + "Action or type is required");
            } else {
                // Validate action/type values
                if (!isValidAction(actionOrType.trim().toLowerCase()) && !isValidType(actionOrType.trim().toUpperCase())) {
                    errors.add(prefix + "Invalid action/type: " + actionOrType + ". Must be one of: create, update, delete (or CREATE_TASK, UPDATE_TASK, DELETE_TASK)");
                }
            }
            
            // Check for entityType field (legacy format) - only required for legacy format
            if (command.getAction() != null && !command.getAction().trim().isEmpty()) {
                if (command.getEntityType() == null || command.getEntityType().trim().isEmpty()) {
                    errors.add(prefix + "Entity type is required");
                }
            }
            
            if (command.getEntityId() == null || command.getEntityId().trim().isEmpty()) {
                errors.add(prefix + "Entity ID is required");
            }
        }
        
        return errors;
    }
    
    private boolean isValidAction(String action) {
        return "create".equals(action) || "update".equals(action) || "delete".equals(action);
    }
    
    private boolean isValidType(String type) {
        return "CREATE_TASK".equals(type) || "UPDATE_TASK".equals(type) || "DELETE_TASK".equals(type);
    }
}