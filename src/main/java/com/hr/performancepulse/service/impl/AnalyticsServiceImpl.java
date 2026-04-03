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
        
        try {
            // 1. Fetch cycle
            ReviewCycle cycle = cycleRepository.findById(cycleId)
                    .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", cycleId));
            log.debug("Fetched cycle: {}", cycle.getName());
            
            // 2. Get total review count
            Long totalReviews = reviewRepository.countByCycleId(cycleId);
            log.debug("Total reviews: {}", totalReviews);
            
            // 3. Calculate average rating (finalized reviews only)
            Double avgRatingRaw = reviewRepository.getAverageRatingByCycleId(cycleId);
            Double avgRating = avgRatingRaw != null ? Math.round(avgRatingRaw * 100.0) / 100.0 : 0.0;
            log.debug("Average rating: {}", avgRating);
            
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
                log.debug("Top performer: {}", topPerformer.getName());
            } else {
                log.debug("No top performer found");
            }
            
            // 5. Get goal statistics
            GoalStatsDTO goalStats = null;
            try {
                goalStats = goalRepository.getGoalStatsByCycleId(cycleId);
            } catch (Exception e) {
                log.warn("Error retrieving goal stats, using defaults: {}", e.getMessage());
                goalStats = GoalStatsDTO.builder()
                        .total(0L)
                        .completed(0L)
                        .missed(0L)
                        .inProgress(0L)
                        .build();
            }
            
            if (goalStats == null) {
                goalStats = GoalStatsDTO.builder()
                        .total(0L)
                        .completed(0L)
                        .missed(0L)
                        .inProgress(0L)
                        .build();
            }
            log.debug("Goal stats: total={}, completed={}, missed={}, inProgress={}", 
                     goalStats.getTotal(), goalStats.getCompleted(), goalStats.getMissed(), goalStats.getInProgress());
            
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
        } catch (Exception e) {
            log.error("Error building cycle summary: {}", e.getMessage(), e);
            throw e;
        }
    }
}
