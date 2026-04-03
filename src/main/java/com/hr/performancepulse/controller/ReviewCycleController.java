package com.hr.performancepulse.controller;

import com.hr.performancepulse.dto.request.CreateCycleRequest;
import com.hr.performancepulse.dto.request.UpdateCycleStatusRequest;
import com.hr.performancepulse.dto.response.ApiResponse;
import com.hr.performancepulse.dto.response.CycleResponse;
import com.hr.performancepulse.dto.response.CycleSummaryResponse;
import com.hr.performancepulse.service.AnalyticsService;
import com.hr.performancepulse.service.ReviewCycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for review cycle management.
 * 
 * Base path: /api/v1/cycles
 * 
 * LLD §8.2 – ReviewCycle endpoints
 */
@RestController
@RequestMapping("/api/v1/cycles")
@RequiredArgsConstructor
@Tag(name = "Review Cycles", description = "Review cycle management endpoints")
public class ReviewCycleController extends BaseController {
    
    private final ReviewCycleService reviewCycleService;
    private final AnalyticsService analyticsService;
    
    /**
     * Create a new review cycle.
     * 
     * POST /api/v1/cycles
     * 
     * @param request the cycle creation request
     * @return 201 Created with CycleResponse
     * @throws DuplicateReviewException 409 if cycle name already exists
     * @throws IllegalArgumentException 400 if endDate <= startDate
     */
    @PostMapping
    @Operation(summary = "Create a new review cycle", description = "Creates a new review cycle with date validation")
    public ResponseEntity<ApiResponse<CycleResponse>> createCycle(
            @Valid @RequestBody CreateCycleRequest request) {
        CycleResponse response = reviewCycleService.createCycle(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }
    
    /**
     * Get a specific review cycle by ID.
     * 
     * GET /api/v1/cycles/{id}
     * 
     * @param id the cycle ID
     * @return 200 OK with CycleResponse
     * @throws ResourceNotFoundException 404 if cycle not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get review cycle by ID", description = "Retrieves a specific review cycle by its unique identifier")
    public ResponseEntity<ApiResponse<CycleResponse>> getCycle(@PathVariable String id) {
        CycleResponse response = reviewCycleService.getCycle(parseUuid(id));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Update the status of a review cycle.
     * 
     * PATCH /api/v1/cycles/{id}/status
     * 
     * @param id the cycle ID
     * @param request the status update request
     * @return 200 OK with updated CycleResponse
     * @throws ResourceNotFoundException 404 if cycle not found
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update cycle status", description = "Updates the status of a specific review cycle")
    public ResponseEntity<ApiResponse<CycleResponse>> updateCycleStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateCycleStatusRequest request) {
        CycleResponse response = reviewCycleService.updateCycleStatus(parseUuid(id), request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get comprehensive summary for a review cycle.
     * 
     * GET /api/v1/cycles/{id}/summary
     * 
     * Returns aggregated metrics:
     * - Average rating (finalized reviews only, rounded to 2 decimals)
     * - Top performer by average rating
     * - Goal statistics (total, completed, missed, in_progress, completion_rate)
     * - Total review count
     * 
     * Response is cached for 10 minutes.
     * 
     * @param id the cycle ID
     * @return 200 OK with CycleSummaryResponse
     * @throws ResourceNotFoundException 404 if cycle not found
     */
    @GetMapping("/{id}/summary")
    @Operation(summary = "Get cycle summary", description = "Get comprehensive analytics summary for a review cycle")
    public ResponseEntity<ApiResponse<CycleSummaryResponse>> getCycleSummary(@PathVariable String id) {
        CycleSummaryResponse summary = analyticsService.buildCycleSummary(parseUuid(id));
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
