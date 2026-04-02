package com.hr.performancepulse.entity;

import com.hr.performancepulse.enums.ReviewType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PerformanceReview entity representing a review submitted by one person for another.
 * 
 * Placeholder for Phase 2 implementation.
 * 
 * LLD §3.4 – PerformanceReview entity design with unique constraint
 * on (employee, cycle, reviewer) combination.
 */
@Entity
@Table(
    name = "performance_reviews",
    indexes = {
        @Index(columnList = "employee_id, cycle_id", name = "idx_review_employee_cycle"),
        @Index(columnList = "reviewer_id", name = "idx_review_reviewer"),
        @Index(columnList = "cycle_id", name = "idx_review_cycle")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceReview extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private ReviewCycle cycle;
    
    /**
     * The person submitting the review (null for self-review).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Employee reviewer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewType reviewType;
    
    @Column(length = 1000)
    private String feedback;
    
    @Column(nullable = false)
    private Integer rating;
    
    private LocalDateTime submittedAt;
    
    // Placeholder: More fields to be added in Phase 2
}
