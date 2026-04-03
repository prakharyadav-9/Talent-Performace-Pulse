package com.hr.performancepulse.service;

import com.hr.performancepulse.dto.request.SubmitReviewRequest;
import com.hr.performancepulse.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for performance review operations.
 * 
 * LLD §6.2 – PerformanceReviewService behavior
 */
public interface PerformanceReviewService {
    
    /**
     * Submit a performance review.
     * 
     * Validates:
     * - Cycle is ACTIVE (422 InvalidCycleStateException)
     * - Employee is ACTIVE (422)
     * - Reviewer is ACTIVE if provided (422)
     * - No duplicate (employee + cycle + reviewer) exists (409 DuplicateReviewException)
     * - Rating is 1-5 (400 InvalidRatingException)
     * 
     * Sets submittedAt to current time in service layer.
     * Evicts cycle-summary and top-performers caches.
     * 
     * @param request submission details
     * @return ReviewResponse with submitted review data
     */
    ReviewResponse submitReview(SubmitReviewRequest request);
    
    /**
     * Get all reviews for an employee across all cycles.
     * 
     * Ordered by most recent first (submittedAt DESC).
     * Includes full cycle details for context.
     * 
     * @param employeeId employee UUID
     * @param pageable pagination info
     * @return Page of ReviewResponse
     * @throws com.hr.performancepulse.exception.ResourceNotFoundException if employee not found
     */
    Page<ReviewResponse> getReviewsByEmployee(UUID employeeId, Pageable pageable);
    
    /**
     * Finalize a review, making it count towards analytics.
     * 
     * Once finalized, a review cannot be edited (throws ReviewFinalizedException on edit attempts).
     * Evicts cycle-summary cache to recalculate analytics.
     * 
     * @param reviewId review UUID to finalize
     * @return ReviewResponse with finalized status
     * @throws com.hr.performancepulse.exception.ResourceNotFoundException if review not found
     */
    ReviewResponse finalizeReview(UUID reviewId);
}
