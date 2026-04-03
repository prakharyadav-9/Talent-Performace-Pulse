package com.hr.performancepulse.repository;

import com.hr.performancepulse.dto.TopPerformerDTO;
import com.hr.performancepulse.entity.PerformanceReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PerformanceReview entity.
 * 
 * LLD §5.2 – PerformanceReviewRepository interface
 */
@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {
    
    /**
     * Find all reviews for a cycle with eager-loaded employees to prevent N+1.
     * Used for building cycle summaries.
     */
    @Query("SELECT r FROM PerformanceReview r " +
           "JOIN FETCH r.employee e " +
           "WHERE r.cycle.id = :cycleId " +
           "ORDER BY r.submittedAt DESC")
    List<PerformanceReview> findByCycleIdWithEmployees(@Param("cycleId") UUID cycleId);
    
    /**
     * Check for duplicate review (same employee, cycle, and reviewer).
     */
    Optional<PerformanceReview> findByEmployeeIdAndCycleIdAndReviewerId(
        UUID employeeId, UUID cycleId, UUID reviewerId);
    
    /**
     * Get paginated reviews for an employee across all cycles.
     * Ordered by most recent first.
     */
    Page<PerformanceReview> findByEmployeeIdOrderBySubmittedAtDesc(UUID employeeId, Pageable pageable);
    
    /**
     * Calculate average rating for a cycle (only finalized reviews).
     * Used for cycle analytics.
     */
    @Query("SELECT AVG(r.rating) FROM PerformanceReview r " +
           "WHERE r.cycle.id = :cycleId AND r.isFinalized = true")
    Double getAverageRatingByCycleId(@Param("cycleId") UUID cycleId);
    
    /**
     * Get the top-performing employee in a cycle based on average rating (as a Map).
     * Returns null if no finalized reviews exist.
     * Used for cycle analytics (Phase 3).
     */
    @Query(nativeQuery = true, value = """
        SELECT e.id, e.first_name, e.last_name, AVG(pr.rating)::NUMERIC as average_rating
        FROM performance_reviews pr
        JOIN employees e ON pr.employee_id = e.id
        WHERE pr.cycle_id = :cycleId AND pr.is_finalized = true
        GROUP BY e.id, e.first_name, e.last_name
        ORDER BY average_rating DESC
        LIMIT 1
        """)
    Map<String, Object> getTopPerformerByCycleIdMap(@Param("cycleId") UUID cycleId);
    
    /**
     * Get top performer as DTO wrapper around map query.
     */
    default TopPerformerDTO getTopPerformerByCycleId(UUID cycleId) {
        Map<String, Object> result = getTopPerformerByCycleIdMap(cycleId);
        if (result == null || result.isEmpty()) {
            return null;
        }
        return TopPerformerDTO.builder()
                .id((java.util.UUID) result.get("id"))
                .firstName((String) result.get("first_name"))
                .lastName((String) result.get("last_name"))
                .averageRating(((Number) result.get("average_rating")).doubleValue())
                .build();
    }
    
    /**
     * Count reviews for a cycle.
     */
    long countByCycleId(UUID cycleId);
}
