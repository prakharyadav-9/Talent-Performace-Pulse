package com.hr.performancepulse.dto.response;

import com.hr.performancepulse.enums.ReviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a performance review.
 * 
 * Includes full cycle details to provide context for the review.
 * LLD §6.2 – API response contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    
    private UUID id;
    
    private UUID employeeId;
    
    private String employeeName;  // firstName + " " + lastName
    
    private UUID cycleId;
    
    private String cycleName;
    
    private LocalDate cycleStartDate;
    
    private LocalDate cycleEndDate;
    
    private UUID reviewerId;
    
    private String reviewerName;  // firstName + " " + lastName (nullable for self-review)
    
    private ReviewType reviewType;
    
    private Integer rating;
    
    private String notes;
    
    private LocalDateTime submittedAt;
    
    private Boolean isFinalized;
}
