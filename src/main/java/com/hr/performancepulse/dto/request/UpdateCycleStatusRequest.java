package com.hr.performancepulse.dto.request;

import com.hr.performancepulse.enums.CycleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a review cycle's status.
 * 
 * LLD §3.3 – ReviewCycle status update
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCycleStatusRequest {
    
    @NotNull(message = "Cycle status is required")
    private CycleStatus status;
}
