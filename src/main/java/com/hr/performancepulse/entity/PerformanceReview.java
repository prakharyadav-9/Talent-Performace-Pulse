package com.hr.performancepulse.entity;

import com.hr.performancepulse.enums.ReviewType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PerformanceReview entity representing a review submitted by one person for another.
 * 
 * Unique constraint on (employee_id, cycle_id, reviewer_id) combination
 * to prevent duplicate reviews in the same cycle.
 * 
 * LLD §3.4 – PerformanceReview entity design
 */
@Entity
@Table(
    name = "performance_reviews",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"employee_id", "cycle_id", "reviewer_id"},
        name = "uk_review_employee_cycle_reviewer"
    ),
    indexes = {
        @Index(columnList = "cycle_id", name = "idx_review_cycle_id"),
        @Index(columnList = "employee_id", name = "idx_review_employee_id"),
        @Index(columnList = "cycle_id, rating DESC", name = "idx_review_rating"),
        @Index(columnList = "is_finalized, cycle_id", name = "idx_review_finalized")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    
    @Column(nullable = false)
    private Integer rating;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewType reviewType;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;
    
    @Column(nullable = false)
    private Boolean isFinalized = false;
}
