package com.registration.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersistenceResponse {
    private Integer dni;
    private String status; // "SUCCESS" or "FAILED"
    private String message;
    private String timestamp;
} 