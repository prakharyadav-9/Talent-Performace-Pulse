package com.hr.performancepulse.dto.response;

import com.hr.performancepulse.enums.CycleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for review cycle details.
 * 
 * LLD §6.2 – ReviewCycle representation in responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CycleResponse {
    
    private UUID id;
    
    private String name;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private CycleStatus status;
    
    private LocalDateTime createdAt;
}
