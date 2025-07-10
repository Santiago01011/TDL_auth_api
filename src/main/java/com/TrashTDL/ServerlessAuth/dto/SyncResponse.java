package com.TrashTDL.ServerlessAuth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SyncResponse {
    private List<Object> success;
    private List<Object> conflicts;
    private List<Object> failed;
}