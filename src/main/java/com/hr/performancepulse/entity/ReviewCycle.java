package com.hr.performancepulse.entity;

import com.hr.performancepulse.enums.CycleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ReviewCycle entity representing a performance review cycle.
 * 
 * Extends {@link AuditEntity} to inherit audit trail and optimistic locking.
 * 
 * LLD §3.3 – ReviewCycle entity design with date range validation
 * and relationships to reviews and goals.
 */
@Entity
@Table(
    name = "review_cycles",
    uniqueConstraints = @UniqueConstraint(columnNames = "name", name = "uq_cycle_name")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCycle extends AuditEntity {
    
    @Column(length = 100, unique = true, nullable = false)
    private String name;
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @Column(nullable = false)
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CycleStatus status = CycleStatus.UPCOMING;
    
    /**
     * One-to-many relationship with performance reviews.
     * Cascade all operations (persist, update, delete).
     * Orphan removal: delete reviews when cycle is deleted.
     */
    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PerformanceReview> reviews = new ArrayList<>();
    
    /**
     * One-to-many relationship with goals.
     * Cascade all operations (persist, update, delete).
     * Orphan removal: delete goals when cycle is deleted.
     */
    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Goal> goals = new ArrayList<>();
}
