package com.hr.performancepulse.exception;

/**
 * Thrown when the same reviewer attempts to submit a second review
 * for the same employee in the same cycle.
 *
 * <p>LLD §10 – maps to HTTP 409 CONFLICT / DUPLICATE_REVIEW.
 * LLD §11 – enforced in PerformanceReviewServiceImpl.submitReview().
 */
public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(String employeeId, String cycleId) {
        super(String.format(
                "A review already exists for employee '%s' in cycle '%s' from this reviewer.",
                employeeId, cycleId));
    }
}
