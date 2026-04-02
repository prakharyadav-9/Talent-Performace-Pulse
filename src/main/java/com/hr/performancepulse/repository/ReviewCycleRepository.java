package com.hr.performancepulse.repository;

import com.hr.performancepulse.entity.ReviewCycle;
import com.hr.performancepulse.enums.CycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ReviewCycle} entity.
 * 
 * LLD §5.2 – ReviewCycleRepository interface
 */
@Repository
public interface ReviewCycleRepository extends JpaRepository<ReviewCycle, UUID> {
    
    /**
     * Find a cycle by its unique name.
     */
    Optional<ReviewCycle> findByName(String name);
    
    /**
     * Find an active cycle (only one should exist at a time).
     */
    Optional<ReviewCycle> findByStatus(CycleStatus status);
}
