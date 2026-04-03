package com.hr.performancepulse.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a goal.
 * 
 * LLD §6.4 – Goal creation validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGoalRequest {
    
    @NotNull(message = "Employee ID is required")
    private UUID employeeId;
    
    @NotNull(message = "Cycle ID is required")
    private UUID cycleId;
    
    @NotBlank(message = "Goal title is required")
    @Size(max = 255, message = "Goal title cannot exceed 255 characters")
    private String title;
    
    @Size(max = 4000, message = "Goal description cannot exceed 4000 characters")
    private String description;
    
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
}
