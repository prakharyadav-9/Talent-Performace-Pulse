package com.hr.performancepulse.repository;

import com.hr.performancepulse.dto.GoalStatsDTO;
import com.hr.performancepulse.entity.Goal;
import com.hr.performancepulse.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
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
     * Uses native query with cast to Map for flexibility.
     */
    @Query(nativeQuery = true, value = """
        SELECT 
            COUNT(*)::BIGINT as total,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END)::BIGINT as completed,
            SUM(CASE WHEN status = 'MISSED' THEN 1 ELSE 0 END)::BIGINT as missed,
            SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END)::BIGINT as in_progress
        FROM goals
        WHERE cycle_id = :cycleId
        """)
    Map<String, Long> getGoalStatsByCycleIdMap(@Param("cycleId") UUID cycleId);
    
    /**
     * Get goal statistics for a cycle (total, completed, missed, in_progress counts).
     * Wrapper method that converts map to DTO.
     */
    default GoalStatsDTO getGoalStatsByCycleId(UUID cycleId) {
        Map<String, Long> result = getGoalStatsByCycleIdMap(cycleId);
        if (result == null || result.isEmpty()) {
            return GoalStatsDTO.builder()
                    .total(0L)
                    .completed(0L)
                    .missed(0L)
                    .inProgress(0L)
                    .build();
        }
        return GoalStatsDTO.builder()
                .total(result.getOrDefault("total", 0L))
                .completed(result.getOrDefault("completed", 0L))
                .missed(result.getOrDefault("missed", 0L))
                .inProgress(result.getOrDefault("in_progress", 0L))
                .build();
    }
}
