package com.hr.performancepulse.dto.request;

import com.hr.performancepulse.enums.ReviewType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for submitting a performance review.
 * 
 * LLD §6.2 – PerformanceReviewService validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitReviewRequest {
    
    @NotNull(message = "Employee ID is required")
    private UUID employeeId;
    
    @NotNull(message = "Cycle ID is required")
    private UUID cycleId;
    
    // Optional for self-review
    private UUID reviewerId;
    
    @NotNull(message = "Review type is required")
    private ReviewType reviewType;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
    
    @Size(max = 4000, message = "Notes cannot exceed 4000 characters")
    private String notes;
}
