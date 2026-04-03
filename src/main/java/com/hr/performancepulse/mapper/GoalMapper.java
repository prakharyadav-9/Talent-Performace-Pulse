package com.hr.performancepulse.mapper;

import com.hr.performancepulse.dto.response.GoalResponse;
import com.hr.performancepulse.entity.Goal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Goal entity to GoalResponse DTO.
 */
@Mapper(componentModel = "spring")
public interface GoalMapper {
    
    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "cycleId", source = "cycle.id")
    GoalResponse toResponse(Goal goal);
}
