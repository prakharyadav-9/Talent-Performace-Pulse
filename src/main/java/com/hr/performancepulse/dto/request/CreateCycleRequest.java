package com.hr.performancepulse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating a new review cycle.
 * 
 * LLD §3.3 – ReviewCycle creation with date validation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCycleRequest {
    
    @NotBlank(message = "Cycle name is required")
    @Size(max = 100, message = "Cycle name must not exceed 100 characters")
    private String name;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    // Validation: endDate must be > startDate
    // Enforced in service layer
}
