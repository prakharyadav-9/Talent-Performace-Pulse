package com.hr.performancepulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Projection DTO for goal statistics in a cycle.
 * 
 * Used by AnalyticsService for cycle summary.
 * LLD §9.3 – Goal stats projection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalStatsDTO {
    
    private Long total;
    
    private Long completed;
    
    private Long missed;
    
    private Long inProgress;
    
    public double getCompletionRate() {
        if (total == null || total == 0) {
            return 0.0;
        }
        return (double) completed / total * 100;
    }
}
