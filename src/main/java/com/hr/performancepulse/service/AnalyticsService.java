package com.hr.performancepulse.service;

import com.hr.performancepulse.dto.response.CycleSummaryResponse;

import java.util.UUID;

/**
 * Service interface for analytics and cycle summaries.
 * 
 * LLD §6.4 – AnalyticsService behavior
 */
public interface AnalyticsService {
    
    /**
     * Build a comprehensive summary for a review cycle.
     * 
     * Aggregates:
     * - Average rating from finalized reviews (rounded to 2 decimals)
     * - Top performer by average rating
     * - Goal statistics (total, completed, missed, in_progress, completion_rate)
     * - Total review count
     * 
     * Results are cached for 10 minutes.
     * Cache is evicted when reviews or goals are modified.
     * 
     * @param cycleId cycle UUID
     * @return CycleSummaryResponse with aggregate metrics
     * @throws ResourceNotFoundException if cycle not found
     */
    CycleSummaryResponse buildCycleSummary(UUID cycleId);
}
