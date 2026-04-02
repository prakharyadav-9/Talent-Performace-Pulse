package com.hr.performancepulse.controller;

import com.hr.performancepulse.dto.request.CreateEmployeeRequest;
import com.hr.performancepulse.dto.response.ApiResponse;
import com.hr.performancepulse.dto.response.EmployeeResponse;
import com.hr.performancepulse.enums.Department;
import com.hr.performancepulse.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for employee management.
 * 
 * Base path: /api/v1/employees
 * 
 * LLD §8.1 – Employee endpoints
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management endpoints")
public class EmployeeController extends BaseController {
    
    private final EmployeeService employeeService;
    
    /**
     * Create a new employee.
     * 
     * POST /api/v1/employees
     * 
     * @param request the employee creation request
     * @return 201 Created with EmployeeResponse
     * @throws DuplicateReviewException 409 if email already exists
     */
    @PostMapping
    @Operation(summary = "Create a new employee", description = "Creates a new employee with email validation")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }
    
    /**
     * List employees with optional filtering and pagination.
     * 
     * GET /api/v1/employees?department=ENGINEERING&minRating=3.5&page=0&size=20
     * 
     * @param department optional department filter
     * @param minRating optional minimum average rating filter
     * @param pageable pagination details (page, size)
     * @return 200 OK with paginated employee list
     */
    @GetMapping
    @Operation(summary = "List employees", description = "Retrieves a paginated list of employees with optional department and rating filters")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> listEmployees(
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) Double minRating,
            Pageable pageable) {
        Page<EmployeeResponse> response = employeeService.listEmployees(department, minRating, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get a specific employee by ID.
     * 
     * GET /api/v1/employees/{id}
     * 
     * @param id the employee ID
     * @return 200 OK with EmployeeResponse
     * @throws ResourceNotFoundException 404 if employee not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID", description = "Retrieves a specific employee by their unique identifier")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable String id) {
        EmployeeResponse response = employeeService.getEmployee(parseUuid(id));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
