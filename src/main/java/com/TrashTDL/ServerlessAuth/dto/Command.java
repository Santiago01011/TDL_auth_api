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
    private String action;
    private String entityType;
    private String entityId;
    private Object data;
    private String clientTimestamp;
}