package com.hr.performancepulse.dto.request;

import com.hr.performancepulse.enums.Department;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new employee.
 * 
 * LLD §3.2 – Employee creation with validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest {
    
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @NotNull(message = "Department is required")
    private Department department;
    
    @NotBlank(message = "Job title is required")
    @Size(max = 150, message = "Job title must not exceed 150 characters")
    private String jobTitle;
    
    // Optional: manager can be null for top-level employees
    private UUID managerId;
    
    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date must be today or in the past")
    private LocalDate joiningDate;
}
