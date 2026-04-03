package com.hr.performancepulse.repository;

import com.hr.performancepulse.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Employee} entity.
 * 
 * Extends JpaSpecificationExecutor for dynamic filtering by department and rating.
 * 
 * LLD §5.1 – EmployeeRepository interface
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    
    /**
     * Check if an employee with the given email already exists.
     * Used to validate email uniqueness before creating a new employee.
     */
    boolean existsByEmail(String email);
    
    /**
     * Find an employee by ID with reviews eagerly loaded.
     * Prevents N+1 query problem when accessing reviews.
     */
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.reviews WHERE e.id = :id")
    Optional<Employee> findByIdWithReviews(@Param("id") UUID id);
    

}
