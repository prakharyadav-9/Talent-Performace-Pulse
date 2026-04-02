package com.hr.performancepulse.mapper;

import com.hr.performancepulse.dto.request.CreateEmployeeRequest;
import com.hr.performancepulse.dto.response.EmployeeResponse;
import com.hr.performancepulse.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Employee entity and DTOs.
 * 
 * Handles bidirectional mapping between:
 * - Employee entity ← CreateEmployeeRequest
 * - Employee entity → EmployeeResponse
 */
@Mapper(componentModel = "spring")
public interface EmployeeMapper {
    
    /**
     * Map CreateEmployeeRequest to Employee entity.
     * Audit fields and relationships are set separately in service layer.
     */
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "goals", ignore = true)
    Employee toEntity(CreateEmployeeRequest request);
    
    /**
     * Map Employee entity to EmployeeResponse.
     * Note: averageRating is computed in service layer.
     */
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "averageRating", ignore = true)
    EmployeeResponse toResponse(Employee employee);
}
