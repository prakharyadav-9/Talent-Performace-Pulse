package com.hr.performancepulse.entity;

import com.hr.performancepulse.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Goal entity representing a goal set for an employee in a review cycle.
 * 
 * Placeholder for Phase 3 implementation.
 * 
 * LLD §3.5 – Goal entity design with progress tracking
 * and cycle-based lifecycle.
 */
@Entity
@Table(
    name = "goals",
    indexes = {
        @Index(columnList = "employee_id, cycle_id", name = "idx_goal_employee_cycle"),
        @Index(columnList = "status", name = "idx_goal_status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private ReviewCycle cycle;
    
    @Column(length = 255, nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.PENDING;
    
    @Column(length = 500)
    private String progressNotes;
    
    private Integer progressPercentage = 0;
    
    // Placeholder: More fields to be added in Phase 3
}
