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
            
            if (command.getAction() == null || command.getAction().trim().isEmpty()) {
                errors.add(prefix + "Action is required");
            }
            
            if (command.getEntityType() == null || command.getEntityType().trim().isEmpty()) {
                errors.add(prefix + "Entity type is required");
            }
            
            if (command.getEntityId() == null || command.getEntityId().trim().isEmpty()) {
                errors.add(prefix + "Entity ID is required");
            }
            
            // Validate action values (only if action is not empty)
            String action = command.getAction();
            if (action != null && !action.trim().isEmpty() && !isValidAction(action.trim().toLowerCase())) {
                errors.add(prefix + "Invalid action: " + action + ". Must be one of: create, update, delete");
            }
        }
        
        return errors;
    }
    
    private boolean isValidAction(String action) {
        return "create".equals(action) || "update".equals(action) || "delete".equals(action);
    }
}