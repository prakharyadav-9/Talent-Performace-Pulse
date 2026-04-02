package com.hr.performancepulse.service;

import com.hr.performancepulse.dto.request.CreateCycleRequest;
import com.hr.performancepulse.dto.response.CycleResponse;

import java.util.UUID;

/**
 * Service interface for review cycle management.
 * 
 * LLD §6.2 – ReviewCycleService behavior specification
 */
public interface ReviewCycleService {
    
    /**
     * Create a new review cycle.
     * 
     * @param request the cycle creation request
     * @return the created cycle response
     * @throws DuplicateReviewException if cycle name already exists
     * @throws IllegalArgumentException if endDate <= startDate
     */
    CycleResponse createCycle(CreateCycleRequest request);
    
    /**
     * Get a cycle by ID.
     * 
     * @param id the cycle ID
     * @return the cycle response
     * @throws ResourceNotFoundException if cycle not found
     */
    CycleResponse getCycle(UUID id);
    
    /**
     * Check if there is an active cycle.
     * Used by review service to validate that reviews are submitted during an active cycle.
     * 
     * @return true if an active cycle exists, false otherwise
     */
    boolean isActiveCycleExists();
    
    /**
     * Update the status of a cycle.
     * 
     * @param id the cycle ID
     * @param newStatus the new status
     * @return the updated cycle response
     * @throws ResourceNotFoundException if cycle not found
     */
    CycleResponse updateCycleStatus(java.util.UUID id, com.hr.performancepulse.enums.CycleStatus newStatus);
}
