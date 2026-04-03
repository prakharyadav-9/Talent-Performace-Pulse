package com.hr.performancepulse.dto.request;

import com.hr.performancepulse.enums.GoalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating a goal.
 * 
 * Allows updating:
 * - title (optional)
 * - description (optional)
 * - dueDate (optional, must be within cycle date range)
 * - status (optional, must be valid GoalStatus enum)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGoalRequest {
    
    @NotBlank(message = "Title cannot be blank")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
    
    @NotNull(message = "Status is required")
    private GoalStatus status;
}
