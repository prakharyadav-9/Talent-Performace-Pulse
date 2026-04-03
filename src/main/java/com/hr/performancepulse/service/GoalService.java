package com.hr.performancepulse.service;

import com.hr.performancepulse.dto.request.CreateGoalRequest;
import com.hr.performancepulse.dto.request.UpdateGoalRequest;
import com.hr.performancepulse.dto.response.GoalResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for goal operations.
 * 
 * LLD §6.4 – GoalService behavior
 */
public interface GoalService {
    
    /**
     * Create a new goal.
     * 
     * Validates:
     * - Employee exists and is ACTIVE (422)
     * - Cycle exists and is not CLOSED (422)
     * - dueDate is within [cycle.startDate, cycle.endDate] (400)
     * 
     * Sets status = PENDING and weight = 1.
     * Evicts cycle-summary cache.
     * 
     * @param request goal creation details
     * @return GoalResponse with created goal
     */
    GoalResponse createGoal(CreateGoalRequest request);
    
    /**
     * Get all goals for an employee in a cycle.
     * 
     * @param employeeId employee UUID
     * @param cycleId cycle UUID
     * @return List of GoalResponse
     */
    List<GoalResponse> getGoalsForCycle(UUID employeeId, UUID cycleId);
    
    /**
     * Update an existing goal.
     * 
     * Validates:
     * - Goal exists (404 ResourceNotFoundException)
     * - dueDate is within [cycle.startDate, cycle.endDate] (400 IllegalArgumentException)
     * - Status is valid GoalStatus enum (400 validation error)
     * 
     * Evicts cycle-summary cache on success.
     * 
     * @param goalId UUID of the goal to update
     * @param request update details (title, description, dueDate, status)
     * @return GoalResponse with updated goal
     */
    GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request);
}
