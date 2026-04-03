package com.hr.performancepulse.service.impl;

import com.hr.performancepulse.dto.request.SubmitReviewRequest;
import com.hr.performancepulse.dto.response.ReviewResponse;
import com.hr.performancepulse.entity.Employee;
import com.hr.performancepulse.entity.PerformanceReview;
import com.hr.performancepulse.entity.ReviewCycle;
import com.hr.performancepulse.enums.CycleStatus;
import com.hr.performancepulse.enums.EmployeeStatus;
import com.hr.performancepulse.exception.*;
import com.hr.performancepulse.mapper.ReviewMapper;
import com.hr.performancepulse.repository.EmployeeRepository;
import com.hr.performancepulse.repository.PerformanceReviewRepository;
import com.hr.performancepulse.repository.ReviewCycleRepository;
import com.hr.performancepulse.service.PerformanceReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of PerformanceReviewService.
 * 
 * LLD §6.2 – Business validation rules and service behavior
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PerformanceReviewServiceImpl implements PerformanceReviewService {
    
    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final ReviewCycleRepository cycleRepository;
    private final ReviewMapper reviewMapper;
    
    /**
     * Submit a performance review with comprehensive validation.
     * 
     * Validation order:
     * 1. Cycle exists and is ACTIVE → 422 InvalidCycleStateException
     * 2. Employee exists and is ACTIVE → 422 InvalidCycleStateException
     * 3. Reviewer (if provided) exists and is ACTIVE → 422 InvalidCycleStateException
     * 4. Rating is 1-5 → 400 InvalidRatingException
     * 5. No duplicate review of (employee, cycle, reviewer) → 409 DuplicateReviewException
     * 
     * Sets submittedAt to current time.
     * Evicts cycle-summary and top-performers caches.
     */
    @Override
    @CacheEvict(value = {"cycle-summary", "top-performers"}, allEntries = true)
    public ReviewResponse submitReview(SubmitReviewRequest request) {
        log.info("Submitting review for employee: {} in cycle: {}", 
                 request.getEmployeeId(), request.getCycleId());
        
        // 1. Validate cycle exists and is ACTIVE
        ReviewCycle cycle = cycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", request.getCycleId()));
        
        if (!cycle.getStatus().equals(CycleStatus.ACTIVE)) {
            log.warn("Cannot submit review for non-ACTIVE cycle: {} (status={})", 
                     request.getCycleId(), cycle.getStatus());
            throw new InvalidCycleStateException(
                "Cannot submit review for cycle in " + cycle.getStatus() + " status. Cycle must be ACTIVE.");
        }
        
        // 2. Validate employee exists and is ACTIVE
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));
        
        if (!employee.getStatus().equals(EmployeeStatus.ACTIVE)) {
            log.warn("Cannot submit review for non-ACTIVE employee: {} (status={})", 
                     request.getEmployeeId(), employee.getStatus());
            throw new InvalidCycleStateException(
                "Employee being reviewed must be ACTIVE. Current status: " + employee.getStatus());
        }
        
        // 3. Validate reviewer (if provided) exists and is ACTIVE
        Employee reviewer = null;
        if (request.getReviewerId() != null) {
            reviewer = employeeRepository.findById(request.getReviewerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (reviewer)", request.getReviewerId()));
            
            if (!reviewer.getStatus().equals(EmployeeStatus.ACTIVE)) {
                log.warn("Cannot submit review from non-ACTIVE reviewer: {} (status={})", 
                         request.getReviewerId(), reviewer.getStatus());
                throw new InvalidCycleStateException(
                    "Reviewer must be ACTIVE. Current status: " + reviewer.getStatus());
            }
        }
        
        // 4. Validate rating is 1-5
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            log.warn("Invalid rating attempt: {}", request.getRating());
            throw new InvalidRatingException(request.getRating() != null ? request.getRating() : 0);
        }
        
        // 5. Check for duplicate review (employee, cycle, reviewer)
        if (reviewRepository.findByEmployeeIdAndCycleIdAndReviewerId(
                request.getEmployeeId(), 
                request.getCycleId(), 
                request.getReviewerId()).isPresent()) {
            log.warn("Duplicate review attempt: employee={}, cycle={}, reviewer={}", 
                     request.getEmployeeId(), request.getCycleId(), request.getReviewerId());
            throw new DuplicateReviewException(
                employee.getFirstName() + " " + employee.getLastName(),
                cycle.getName());
        }
        
        // Create review with submittedAt set to now
        PerformanceReview review = new PerformanceReview();
        review.setEmployee(employee);
        review.setCycle(cycle);
        review.setReviewer(reviewer);
        review.setRating(request.getRating());
        review.setNotes(request.getNotes());
        review.setReviewType(request.getReviewType());
        review.setSubmittedAt(LocalDateTime.now());
        review.setIsFinalized(false);
        
        PerformanceReview saved = reviewRepository.save(review);
        log.info("Review submitted successfully: ID={}, employee={}, cycle={}, rating={}", 
                 saved.getId(), employee.getFirstName() + " " + employee.getLastName(), 
                 cycle.getName(), request.getRating());
        
        return reviewMapper.toResponse(saved);
    }
    
    /**
     * Get paginated reviews for an employee across all cycles.
     * 
     * Orders by submittedAt DESC (most recent first).
     * Includes full cycle details for context.
     * 
     * @param employeeId employee UUID
     * @param pageable pagination
     * @return Page of ReviewResponse
     * @throws ResourceNotFoundException if employee not found
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByEmployee(UUID employeeId, Pageable pageable) {
        log.info("Fetching reviews for employee: {} with pagination: page={}, size={}", 
                 employeeId, pageable.getPageNumber(), pageable.getPageSize());
        
        // Validate employee exists
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", employeeId);
        }
        
        Page<PerformanceReview> reviews = reviewRepository.findByEmployeeIdOrderBySubmittedAtDesc(employeeId, pageable);
        return reviews.map(reviewMapper::toResponse);
    }
}
