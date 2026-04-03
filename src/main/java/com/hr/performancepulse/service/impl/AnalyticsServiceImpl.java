package com.hr.performancepulse.service.impl;

import com.hr.performancepulse.dto.EmployeeSummaryDTO;
import com.hr.performancepulse.dto.GoalStatsDTO;
import com.hr.performancepulse.dto.TopPerformerDTO;
import com.hr.performancepulse.dto.response.CycleSummaryResponse;
import com.hr.performancepulse.entity.ReviewCycle;
import com.hr.performancepulse.exception.ResourceNotFoundException;
import com.hr.performancepulse.repository.GoalRepository;
import com.hr.performancepulse.repository.PerformanceReviewRepository;
import com.hr.performancepulse.repository.ReviewCycleRepository;
import com.hr.performancepulse.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of AnalyticsService.
 * 
 * LLD §6.4 – Analytics calculations and caching
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {
    
    private final PerformanceReviewRepository reviewRepository;
    private final GoalRepository goalRepository;
    private final ReviewCycleRepository cycleRepository;
    
    /**
     * Build comprehensive cycle summary with caching.
     * 
     * Cached for 10 minutes per cycle.
     * Evicted when reviews or goals are created/updated.
     * 
     * @param cycleId cycle UUID
     * @return CycleSummaryResponse with all metrics
     */
    @Override
    @Cacheable(value = "cycle-summary", key = "#cycleId")
    public CycleSummaryResponse buildCycleSummary(UUID cycleId) {
        log.info("Building summary for cycle: {}", cycleId);
        
        // 1. Fetch cycle
        ReviewCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", cycleId));
        
        // 2. Get total review count
        Long totalReviews = reviewRepository.countByCycleId(cycleId);
        
        // 3. Calculate average rating (finalized reviews only)
        Double avgRatingRaw = reviewRepository.getAverageRatingByCycleId(cycleId);
        Double avgRating = avgRatingRaw != null ? Math.round(avgRatingRaw * 100.0) / 100.0 : 0.0;
        
        // 4. Get top performer
        TopPerformerDTO topPerformerRaw = reviewRepository.getTopPerformerByCycleId(cycleId);
        EmployeeSummaryDTO topPerformer = null;
        if (topPerformerRaw != null) {
            Double topRatingRounded = Math.round(topPerformerRaw.getAverageRating() * 100.0) / 100.0;
            topPerformer = EmployeeSummaryDTO.builder()
                    .id(topPerformerRaw.getId())
                    .name(topPerformerRaw.getFullName())
                    .averageRating(topRatingRounded)
                    .build();
        }
        
        // 5. Get goal statistics
        GoalStatsDTO goalStats = convertGoalStats(goalRepository.getGoalStatsByCycleId(cycleId));
        
        // 6. Build response
        CycleSummaryResponse response = CycleSummaryResponse.builder()
                .cycleId(cycleId)
                .cycleName(cycle.getName())
                .startDate(cycle.getStartDate())
                .endDate(cycle.getEndDate())
                .totalReviews(totalReviews)
                .averageRating(avgRating)
                .topPerformer(topPerformer)
                .goalStats(goalStats)
                .generatedAt(LocalDateTime.now())
                .build();
        
        log.info("Summary generated for cycle: {} (avg rating: {}, top performer: {})", 
                 cycleId, avgRating, topPerformer != null ? topPerformer.getName() : "N/A");
        
        return response;
    }
    
    /**
     * Convert raw goal statistics from native query to GoalStatsDTO.
     * Handles NULL values from COUNT and SUM operations.
     * 
     * @param rawData Object[] from native query [total, completed, missed, in_progress]
     * @return GoalStatsDTO with converted values
     */
    private GoalStatsDTO convertGoalStats(Object[] rawData) {
        if (rawData == null || rawData.length == 0) {
            return GoalStatsDTO.builder()
                    .total(0L)
                    .completed(0L)
                    .missed(0L)
                    .inProgress(0L)
                    .build();
        }
        
        try {
            Long total = rawData[0] != null ? ((Number) rawData[0]).longValue() : 0L;
            Long completed = rawData[1] != null ? ((Number) rawData[1]).longValue() : 0L;
            Long missed = rawData[2] != null ? ((Number) rawData[2]).longValue() : 0L;
            Long inProgress = rawData[3] != null ? ((Number) rawData[3]).longValue() : 0L;
            
            return GoalStatsDTO.builder()
                    .total(total)
                    .completed(completed)
                    .missed(missed)
                    .inProgress(inProgress)
                    .build();
        } catch (Exception e) {
            log.error("Error converting goal statistics: {}", e.getMessage(), e);
            return GoalStatsDTO.builder()
                    .total(0L)
                    .completed(0L)
                    .missed(0L)
                    .inProgress(0L)
                    .build();
        }
    }
}
