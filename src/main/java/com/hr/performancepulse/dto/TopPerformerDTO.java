package com.hr.performancepulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Projection DTO for top-performing employees in a cycle.
 * 
 * Used by analytics service to return aggregated data.
 * LLD §6.3 – Analytics projections
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopPerformerDTO {
    
    private UUID id;
    
    private String firstName;
    
    private String lastName;
    
    private Double averageRating;
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
