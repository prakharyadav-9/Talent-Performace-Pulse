package com.hr.performancepulse.dto.response;

import com.hr.performancepulse.enums.Department;
import com.hr.performancepulse.enums.EmployeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for employee details.
 * 
 * LLD §6.1 – Employee representation in responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    
    private UUID id;
    
    private String firstName;
    
    private String lastName;
    
    private String email;
    
    private Department department;
    
    private String jobTitle;
    
    private UUID managerId;
    
    private LocalDate joiningDate;
    
    private EmployeeStatus status;
    
    private Double averageRating;  // Computed from reviews
}
