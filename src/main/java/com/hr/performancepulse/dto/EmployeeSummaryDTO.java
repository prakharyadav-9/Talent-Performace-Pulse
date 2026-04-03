package com.hr.performancepulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Projection DTO for employee summary in analytics responses.
 * 
 * Used in CycleSummaryResponse for top performer info.
 * LLD §9.3 – Employee summary DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSummaryDTO {
    
    private UUID id;
    
    private String name;  // firstName + " " + lastName
    
    private Double averageRating;  // Rounded to 2 decimal places
}
