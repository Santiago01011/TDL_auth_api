package com.TrashTDL.ServerlessAuth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Command {
    private String commandId;
    private String entityId;
    private String type;
    private Object data;
    private String timestamp;
    
    // Legacy fields for backward compatibility
    private String action;
    private String entityType;
    private String clientTimestamp;
}