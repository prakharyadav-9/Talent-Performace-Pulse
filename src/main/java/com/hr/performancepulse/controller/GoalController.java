package com.hr.performancepulse.controller;

import com.hr.performancepulse.dto.request.CreateGoalRequest;
import com.hr.performancepulse.dto.request.UpdateGoalRequest;
import com.hr.performancepulse.dto.response.ApiResponse;
import com.hr.performancepulse.dto.response.GoalResponse;
import com.hr.performancepulse.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for goal endpoints.
 * 
 * LLD §7.3 – Goal management endpoints
 */
@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "APIs for managing performance goals")
public class GoalController extends BaseController {
    
    private final GoalService goalService;
    
    /**
     * Create a new goal.
     * 
     * Validates:
     * - Cycle is not CLOSED (422 InvalidCycleStateException)
     * - Employee is ACTIVE (422)
     * - dueDate within [cycle.startDate, cycle.endDate] (400 IllegalArgumentException)
     * 
     * @param request CreateGoalRequest with goal details
     * @return 201 Created with GoalResponse
     */
    @PostMapping
    @Operation(summary = "Create a goal", description = "Creates a new performance goal for an employee in a cycle")
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(
            @Valid @RequestBody CreateGoalRequest request) {
        GoalResponse response = goalService.createGoal(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
    
    /**
     * Update an existing goal.
     * 
     * Validates:
     * - Goal exists (404 ResourceNotFoundException)
     * - dueDate within [cycle.startDate, cycle.endDate] (400 IllegalArgumentException)
     * - Status is valid GoalStatus enum (400 validation error)
     * 
     * Updates goal fields: title, description, dueDate, status
     * 
     * @param goalId UUID of the goal to update
     * @param request UpdateGoalRequest with new values
     * @return 200 OK with updated GoalResponse
     */
    @PatchMapping("/{goalId}")
    @Operation(summary = "Update a goal", description = "Updates an existing goal including its title, description, due date, and status")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request) {
        GoalResponse response = goalService.updateGoal(goalId, request);
        return ResponseEntity
                .ok()
                .body(ApiResponse.success(response));
    }
}
