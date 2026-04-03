package com.hr.performancepulse.entity;

import com.hr.performancepulse.enums.Department;
import com.hr.performancepulse.enums.EmployeeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee entity representing an employee in the system.
 * 
 * Extends {@link AuditEntity} to inherit audit trail and optimistic locking.
 * 
 * LLD §3.2 – Employee entity design with manager hierarchy,
 * department classification, and review/goal relationships.
 */
@Entity
@Table(
    name = "employees",
    indexes = {
        @Index(columnList = "department, status", name = "idx_employee_dept_status"),
        @Index(columnList = "email", name = "idx_employee_email"),
        @Index(columnList = "manager_id", name = "idx_employee_manager")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends AuditEntity {
    
    /**
     * Note: Using @EqualsAndHashCode(callSuper=true) to include audit fields in equality checks.
     * Lombok's @Data generates equals/hashCode, and callSuper=false is intentional for JPA entities.
     */
    
    @Column(length = 100, nullable = false)
    private String firstName;
    
    @Column(length = 100, nullable = false)
    private String lastName;
    
    @Column(length = 255, unique = true, nullable = false)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;
    
    @Column(length = 150, nullable = false)
    private String jobTitle;
    
    /**
     * Self-referencing foreign key to represent manager hierarchy.
     * Nullable because top-level employees have no manager.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;
    
    @Column(nullable = false)
    private LocalDate joiningDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
    
    /**
     * One-to-many relationship with reviews.
     * Cascade all operations (persist, update, delete).
     * Orphan removal: delete reviews when employee is deleted.
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PerformanceReview> reviews = new ArrayList<>();
    
    /**
     * One-to-many relationship with goals.
     * Cascade all operations (persist, update, delete).
     * Orphan removal: delete goals when employee is deleted.
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Goal> goals = new ArrayList<>();
}
