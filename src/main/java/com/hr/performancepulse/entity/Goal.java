package com.hr.performancepulse.entity;

import com.hr.performancepulse.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Goal entity representing performance goals for an employee in a cycle.
 * 
 * LLD §3.5 – Goal entity design
 */
@Entity
@Table(
    name = "goals",
    indexes = {
        @Index(columnList = "employee_id, cycle_id", name = "idx_goal_employee_cycle"),
        @Index(columnList = "cycle_id, status", name = "idx_goal_status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goal extends AuditEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private ReviewCycle cycle;
    
    @Column(length = 255, nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status = GoalStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column(nullable = false)
    private Integer weight = 1;
}
