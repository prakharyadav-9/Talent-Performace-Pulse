package com.hr.performancepulse.repository;

import com.hr.performancepulse.dto.GoalStatsDTO;
import com.hr.performancepulse.entity.Goal;
import com.hr.performancepulse.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Goal entity.
 * 
 * LLD §5.3 – GoalRepository interface
 */
@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {
    
    /**
     * Find all goals for an employee in a specific cycle.
     */
    List<Goal> findByEmployeeIdAndCycleId(UUID employeeId, UUID cycleId);
    
    /**
     * Find all goals for a cycle with specific statuses.
     */
    List<Goal> findByCycleIdAndStatusIn(UUID cycleId, List<GoalStatus> statuses);
    
    /**
     * Get goal statistics for a cycle (total, completed, missed, in_progress counts).
     * Returns raw data as Object[] to avoid Tuple conversion issues.
     * Index: [0]=total, [1]=completed, [2]=missed, [3]=in_progress
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN status = 'MISSED' THEN 1 ELSE 0 END) as missed,
            SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress
        FROM goals
        WHERE cycle_id = :cycleId
        """)
    Object[] getGoalStatsByCycleId(@Param("cycleId") UUID cycleId);
}
