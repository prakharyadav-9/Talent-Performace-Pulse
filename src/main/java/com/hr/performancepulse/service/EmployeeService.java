package com.hr.performancepulse.service;

import com.hr.performancepulse.dto.request.CreateEmployeeRequest;
import com.hr.performancepulse.dto.response.EmployeeResponse;
import com.hr.performancepulse.enums.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for employee management.
 * 
 * LLD §6.1 – EmployeeService behavior specification
 */
public interface EmployeeService {
    
    /**
     * Create a new employee.
     * 
     * @param request the employee creation request
     * @return the created employee response
     * @throws DuplicateReviewException if email already exists
     */
    EmployeeResponse createEmployee(CreateEmployeeRequest request);
    
    /**
     * Get an employee by ID.
     * 
     * @param id the employee ID
     * @return the employee response
     * @throws ResourceNotFoundException if employee not found
     */
    EmployeeResponse getEmployee(UUID id);
    
    /**
     * List employees with optional filtering and pagination.
     * Results are cached for 5 minutes.
     * 
     * @param department optional department filter
     * @param minRating optional minimum average rating filter
     * @param pageable pagination details
     * @return paginated employee list
     */
    Page<EmployeeResponse> listEmployees(Department department, Double minRating, Pageable pageable);
}
