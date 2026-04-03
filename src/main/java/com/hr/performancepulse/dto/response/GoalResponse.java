package com.hr.performancepulse.dto.response;

import com.hr.performancepulse.enums.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a goal.
 * 
 * LLD §9.3 – Goal response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalResponse {
    
    private UUID id;
    
    private UUID employeeId;
    
    private UUID cycleId;
    
    private String title;
    
    private String description;
    
    private GoalStatus status;
    
    private LocalDate dueDate;
    
    private LocalDateTime completedAt;
    
    private Integer weight;
}
