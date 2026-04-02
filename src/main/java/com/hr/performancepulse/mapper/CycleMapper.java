package com.hr.performancepulse.mapper;

import com.hr.performancepulse.dto.request.CreateCycleRequest;
import com.hr.performancepulse.dto.response.CycleResponse;
import com.hr.performancepulse.entity.ReviewCycle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for ReviewCycle entity and DTOs.
 * 
 * Handles bidirectional mapping between:
 * - ReviewCycle entity ← CreateCycleRequest
 * - ReviewCycle entity → CycleResponse
 */
@Mapper(componentModel = "spring")
public interface CycleMapper {
    
    /**
     * Map CreateCycleRequest to ReviewCycle entity.
     * Status defaults to UPCOMING, relationships are set separately.
     */
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "goals", ignore = true)
    ReviewCycle toEntity(CreateCycleRequest request);
    
    /**
     * Map ReviewCycle entity to CycleResponse.
     */
    CycleResponse toResponse(ReviewCycle cycle);
}
