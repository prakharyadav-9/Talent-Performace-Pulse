package com.hr.performancepulse.mapper;

import com.hr.performancepulse.dto.response.ReviewResponse;
import com.hr.performancepulse.entity.PerformanceReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for PerformanceReview entity to ReviewResponse DTO.
 * 
 * Combines employee names and maps cycle details from relationships.
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper {
    
    @Mapping(target = "employeeName", expression = "java(review.getEmployee().getFirstName() + \" \" + review.getEmployee().getLastName())")
    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "cycleId", source = "cycle.id")
    @Mapping(target = "cycleName", source = "cycle.name")
    @Mapping(target = "cycleStartDate", source = "cycle.startDate")
    @Mapping(target = "cycleEndDate", source = "cycle.endDate")
    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewerName", expression = "java(review.getReviewer() != null ? review.getReviewer().getFirstName() + \" \" + review.getReviewer().getLastName() : null)")
    ReviewResponse toResponse(PerformanceReview review);
}
