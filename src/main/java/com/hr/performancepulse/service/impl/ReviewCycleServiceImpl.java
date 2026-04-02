package com.hr.performancepulse.service.impl;

import com.hr.performancepulse.dto.request.CreateCycleRequest;
import com.hr.performancepulse.dto.response.CycleResponse;
import com.hr.performancepulse.entity.ReviewCycle;
import com.hr.performancepulse.enums.CycleStatus;
import com.hr.performancepulse.exception.DuplicateReviewException;
import com.hr.performancepulse.exception.ResourceNotFoundException;
import com.hr.performancepulse.mapper.CycleMapper;
import com.hr.performancepulse.repository.ReviewCycleRepository;
import com.hr.performancepulse.service.ReviewCycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link ReviewCycleService}.
 * 
 * LLD §6.2 – Review cycle management service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewCycleServiceImpl implements ReviewCycleService {
    
    private final ReviewCycleRepository cycleRepository;
    private final CycleMapper cycleMapper;
    
    /**
     * Create a new review cycle with validation.
     * 
     * Validates:
     * - Cycle name uniqueness (409 Conflict if duplicate)
     * - Date range: endDate > startDate (400 Bad Request if invalid)
     */
    @Override
    @Transactional
    public CycleResponse createCycle(CreateCycleRequest request) {
        // Check name uniqueness
        if (cycleRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateReviewException(request.getName(), "cycle");
        }
        
        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate()) || 
            request.getEndDate().isEqual(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        
        // Create cycle entity from request
        ReviewCycle cycle = cycleMapper.toEntity(request);
        
        // Set default status (required by NOT NULL constraint)
        if (cycle.getStatus() == null) {
            cycle.setStatus(CycleStatus.UPCOMING);
        }
        
        // Persist and return response
        ReviewCycle saved = cycleRepository.save(cycle);
        log.info("Created review cycle: {} with ID: {}", saved.getName(), saved.getId());
        return cycleMapper.toResponse(saved);
    }
    
    /**
     * Get cycle by ID.
     */
    @Override
    public CycleResponse getCycle(UUID id) {
        ReviewCycle cycle = cycleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", id));
        return cycleMapper.toResponse(cycle);
    }
    
    /**
     * Check if an active cycle exists.
     * Used by review service to validate that reviews are submitted during an active cycle.
     */
    @Override
    public boolean isActiveCycleExists() {
        return cycleRepository.findByStatus(CycleStatus.ACTIVE).isPresent();
    }
    
    /**
     * Update the status of a given cycle.
     * 
     * @param id the cycle ID
     * @param newStatus the new status to set
     * @return the updated cycle response
     * @throws ResourceNotFoundException if cycle not found
     */
    @Override
    @Transactional
    public CycleResponse updateCycleStatus(UUID id, CycleStatus newStatus) {
        ReviewCycle cycle = cycleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", id));
        
        cycle.setStatus(newStatus);
        ReviewCycle saved = cycleRepository.save(cycle);
        
        log.info("Updated review cycle: {} (ID: {}) status to: {}", saved.getName(), saved.getId(), newStatus);
        return cycleMapper.toResponse(saved);
    }
}
