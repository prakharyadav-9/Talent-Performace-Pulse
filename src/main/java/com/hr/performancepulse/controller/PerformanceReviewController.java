package com.hr.performancepulse.controller;

import com.hr.performancepulse.dto.request.SubmitReviewRequest;
import com.hr.performancepulse.dto.response.ApiResponse;
import com.hr.performancepulse.dto.response.ReviewResponse;
import com.hr.performancepulse.service.PerformanceReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for performance review endpoints.
 * 
 * LLD §7.2 – API endpoints for review management
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Performance Reviews", description = "APIs for managing performance reviews")
public class PerformanceReviewController extends BaseController {
    
    private final PerformanceReviewService reviewService;
    
    /**
     * Submit a new performance review.
     * 
     * Validates:
     * - Cycle is ACTIVE (422 InvalidCycleStateException)
     * - Employee is ACTIVE (422)
     * - Reviewer is ACTIVE if provided (422)
     * - No duplicate review (409 DuplicateReviewException)
     * - Rating is 1-5 (400 InvalidRatingException)
     * 
     * @param request SubmitReviewRequest with review details
     * @return 201 Created with ReviewResponse
     */
    @PostMapping
    @Operation(summary = "Submit a performance review", description = "Submit a review for an employee in the specified cycle")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @Valid @RequestBody SubmitReviewRequest request) {
        ReviewResponse response = reviewService.submitReview(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
    
    /**
     * Finalize a performance review.
     * 
     * Once finalized, the review counts towards analytics (average rating, etc).
     * Finalized reviews cannot be edited.
     * 
     * @param reviewId UUID of the review to finalize
     * @return 200 OK with ReviewResponse
     */
    @PatchMapping("/{reviewId}/finalize")
    @Operation(summary = "Finalize a review", description = "Finalize a review to make it count towards analytics")
    public ResponseEntity<ApiResponse<ReviewResponse>> finalizeReview(
            @PathVariable java.util.UUID reviewId) {
        ReviewResponse response = reviewService.finalizeReview(reviewId);
        return ResponseEntity
                .ok()
                .body(ApiResponse.success(response));
    }
}
