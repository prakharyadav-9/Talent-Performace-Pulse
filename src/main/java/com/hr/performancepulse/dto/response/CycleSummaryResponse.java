package com.hr.performancepulse.dto.response;

import com.hr.performancepulse.dto.EmployeeSummaryDTO;
import com.hr.performancepulse.dto.GoalStatsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for cycle summary analytics.
 * 
 * Includes aggregate metrics: average rating, top performer, goal stats.
 * LLD §9.3 – Cycle summary response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleSummaryResponse {
    
    private UUID cycleId;
    
    private String cycleName;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Long totalReviews;
    
    private Double averageRating;  // Rounded to 2 decimal places
    
    private EmployeeSummaryDTO topPerformer;  // Highest average rating
    
    private GoalStatsDTO goalStats;  // Total, completed, missed, inProgress + completionRate
    
    private LocalDateTime generatedAt;
}
