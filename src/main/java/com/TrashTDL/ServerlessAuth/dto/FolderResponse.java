package com.TrashTDL.ServerlessAuth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for representing a user's folder in API responses.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FolderResponse {
    private UUID folderId;
    private String folderName;
}
