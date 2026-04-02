package com.hr.performancepulse.service.impl;

import com.hr.performancepulse.dto.request.CreateEmployeeRequest;
import com.hr.performancepulse.dto.response.EmployeeResponse;
import com.hr.performancepulse.entity.Employee;
import com.hr.performancepulse.enums.Department;
import com.hr.performancepulse.enums.EmployeeStatus;
import com.hr.performancepulse.exception.DuplicateEmployeeException;
import com.hr.performancepulse.exception.ResourceNotFoundException;
import com.hr.performancepulse.mapper.EmployeeMapper;
import com.hr.performancepulse.repository.EmployeeRepository;
import com.hr.performancepulse.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link EmployeeService}.
 * 
 * LLD §6.1 – Employee management service with filtering and caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    
    /**
     * Create a new employee with validation.
     * 
     * Validates:
     * - Email uniqueness (409 Conflict if duplicate)
     */
    @Override
    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        // Check email uniqueness
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmployeeException(request.getEmail());
        }
        
        // Create employee entity from request
        Employee employee = employeeMapper.toEntity(request);
        
        // Set default status (required by NOT NULL constraint)
        if (employee.getStatus() == null) {
            employee.setStatus(EmployeeStatus.ACTIVE);
        }
        
        // Resolve manager if provided
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager", request.getManagerId()));
            employee.setManager(manager);
        }
        
        // Persist and return response
        Employee saved = employeeRepository.save(employee);
        log.info("Created employee: {} with ID: {}", saved.getEmail(), saved.getId());
        return employeeMapper.toResponse(saved);
    }
    
    /**
     * Get employee by ID.
     * Eager loads reviews to prevent N+1 issues.
     */
    @Override
    public EmployeeResponse getEmployee(UUID id) {
        Employee employee = employeeRepository.findByIdWithReviews(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        return employeeMapper.toResponse(employee);
    }
    
    /**
     * List employees with optional filtering and pagination.
     * Results cached for 5 minutes.
     * 
     * Supports filtering by:
     * - Department (exact match)
     * - Minimum average rating (requires JOIN with reviews)
     */
    @Override
    @Cacheable(value = "employee-list", key = "#department + '_' + #minRating + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<EmployeeResponse> listEmployees(Department department, Double minRating, Pageable pageable) {
        // Build dynamic specification
        Specification<Employee> spec = buildSpecification(department);
        
        // Fetch employees matching specification
        Page<Employee> employees = employeeRepository.findAll(spec, pageable);
        
        // Map to response and filter by minRating if provided
        List<EmployeeResponse> responses = employees.getContent().stream()
            .map(employee -> {
                EmployeeResponse response = employeeMapper.toResponse(employee);
                // Compute average rating from reviews
                if (!employee.getReviews().isEmpty()) {
                    double avgRating = employee.getReviews().stream()
                        .mapToDouble(r -> r.getRating() == null ? 0 : r.getRating())
                        .average()
                        .orElse(0.0);
                    response.setAverageRating(avgRating);
                }
                return response;
            })
            .filter(response -> minRating == null || response.getAverageRating() == null || response.getAverageRating() >= minRating)
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, employees.getTotalElements());
    }
    
    /**
     * Build dynamic JPA Specification for filtering.
     */
    private Specification<Employee> buildSpecification(Department department) {
        return (root, query, criteriaBuilder) -> {
            if (department != null) {
                return criteriaBuilder.equal(root.get("department"), department);
            }
            return criteriaBuilder.conjunction();
        };
    }
}
