package com.hr.performancepulse.service.impl;

import com.hr.performancepulse.dto.request.CreateGoalRequest;
import com.hr.performancepulse.dto.request.UpdateGoalRequest;
import com.hr.performancepulse.dto.response.GoalResponse;
import com.hr.performancepulse.entity.Employee;
import com.hr.performancepulse.entity.Goal;
import com.hr.performancepulse.entity.ReviewCycle;
import com.hr.performancepulse.enums.CycleStatus;
import com.hr.performancepulse.enums.EmployeeStatus;
import com.hr.performancepulse.enums.GoalStatus;
import com.hr.performancepulse.exception.InvalidCycleStateException;
import com.hr.performancepulse.exception.ResourceNotFoundException;
import com.hr.performancepulse.mapper.GoalMapper;
import com.hr.performancepulse.repository.EmployeeRepository;
import com.hr.performancepulse.repository.GoalRepository;
import com.hr.performancepulse.repository.ReviewCycleRepository;
import com.hr.performancepulse.service.GoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of GoalService.
 * 
 * LLD §6.4 – Business validation rules for goals
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GoalServiceImpl implements GoalService {
    
    private final GoalRepository goalRepository;
    private final EmployeeRepository employeeRepository;
    private final ReviewCycleRepository cycleRepository;
    private final GoalMapper goalMapper;
    
    /**
     * Create a goal with comprehensive validation.
     * 
     * Validation order:
     * 1. Cycle exists and not CLOSED → 422 InvalidCycleStateException
     * 2. Employee exists and is ACTIVE → 422 InvalidCycleStateException
     * 3. dueDate within [cycle.startDate, cycle.endDate] → 400 IllegalArgumentException
     * 
     * Sets status = PENDING and weight = 1.
     * Evicts cycle-summary cache on success.
     */
    @Override
    @CacheEvict(value = "cycle-summary", allEntries = true)
    public GoalResponse createGoal(CreateGoalRequest request) {
        log.info("Creating goal for employee: {} in cycle: {}", 
                 request.getEmployeeId(), request.getCycleId());
        
        // 1. Validate cycle exists and is not CLOSED
        ReviewCycle cycle = cycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCycle", request.getCycleId()));
        
        if (cycle.getStatus().equals(CycleStatus.CLOSED)) {
            log.warn("Cannot create goal for CLOSED cycle: {}", request.getCycleId());
            throw new InvalidCycleStateException(
                "Cannot create goals for a CLOSED cycle. Cycle status: " + cycle.getStatus());
        }
        
        // 2. Validate employee exists and is ACTIVE
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.getEmployeeId()));
        
        if (!employee.getStatus().equals(EmployeeStatus.ACTIVE)) {
            log.warn("Cannot create goal for non-ACTIVE employee: {} (status={})", 
                     request.getEmployeeId(), employee.getStatus());
            throw new InvalidCycleStateException(
                "Employee must be ACTIVE to have goals. Current status: " + employee.getStatus());
        }
        
        // 3. Validate dueDate is within cycle date range
        LocalDate dueDate = request.getDueDate();
        if (dueDate.isBefore(cycle.getStartDate()) || dueDate.isAfter(cycle.getEndDate())) {
            log.warn("Goal due date outside cycle range: due={}, start={}, end={}", 
                     dueDate, cycle.getStartDate(), cycle.getEndDate());
            throw new IllegalArgumentException(
                String.format("Goal due date must be within cycle date range [%s, %s]. Provided: %s",
                    cycle.getStartDate(), cycle.getEndDate(), dueDate));
        }
        
        // Create goal with defaults
        Goal goal = new Goal();
        goal.setEmployee(employee);
        goal.setCycle(cycle);
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setDueDate(dueDate);
        goal.setStatus(GoalStatus.PENDING);
        goal.setWeight(1);
        
        Goal saved = goalRepository.save(goal);
        log.info("Goal created successfully: ID={}, title={}, employee={}, cycle={}", 
                 saved.getId(), saved.getTitle(), 
                 employee.getFirstName() + " " + employee.getLastName(), 
                 cycle.getName());
        
        return goalMapper.toResponse(saved);
    }
    
    /**
     * Get all goals for an employee in a cycle.
     * 
     * @param employeeId employee UUID
     * @param cycleId cycle UUID
     * @return List of GoalResponse
     */
    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsForCycle(UUID employeeId, UUID cycleId) {
        log.info("Fetching goals for employee: {} in cycle: {}", employeeId, cycleId);
        
        List<Goal> goals = goalRepository.findByEmployeeIdAndCycleId(employeeId, cycleId);
        return goals.stream()
                .map(goalMapper::toResponse)
                .toList();
    }
    
    /**
     * Update an existing goal with comprehensive validation.
     * 
     * Validation:
     * 1. Goal exists → 404 ResourceNotFoundException
     * 2. dueDate within [cycle.startDate, cycle.endDate] → 400 IllegalArgumentException
     * 3. Status is valid enum value → validation handled by Spring
     * 
     * Updates: title, description, dueDate, status
     * Evicts cycle-summary cache on success.
     * 
     * @param goalId goal UUID to update
     * @param request update details
     * @return GoalResponse with updated goal
     */
    @Override
    @CacheEvict(value = "cycle-summary", allEntries = true)
    public GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request) {
        log.info("Updating goal: ID={}, new status={}", goalId, request.getStatus());
        
        // 1. Verify goal exists
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", goalId));
        
        ReviewCycle cycle = goal.getCycle();
        
        // 2. Validate new dueDate is within cycle date range
        LocalDate newDueDate = request.getDueDate();
        if (newDueDate.isBefore(cycle.getStartDate()) || newDueDate.isAfter(cycle.getEndDate())) {
            log.warn("Updated goal due date outside cycle range: due={}, start={}, end={}", 
                     newDueDate, cycle.getStartDate(), cycle.getEndDate());
            throw new IllegalArgumentException(
                String.format("Goal due date must be within cycle date range [%s, %s]. Provided: %s",
                    cycle.getStartDate(), cycle.getEndDate(), newDueDate));
        }
        
        // 3. Update goal fields
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setDueDate(newDueDate);
        goal.setStatus(request.getStatus());
        
        // 4. Save and log
        Goal updated = goalRepository.save(goal);
        log.info("Goal updated successfully: ID={}, title={}, new status={}", 
                 updated.getId(), updated.getTitle(), updated.getStatus());
        
        return goalMapper.toResponse(updated);
    }
}
