package com.hr.performancepulse.exception;

import java.util.UUID;

/**
 * Thrown when a PATCH or DELETE is attempted on a finalised review.
 *
 * <p>LLD §10 – maps to HTTP 409 / REVIEW_FINALIZED.
 * LLD §11 – enforced in PerformanceReviewServiceImpl once isFinalized=true.
 */
public class ReviewFinalizedException extends RuntimeException {
    public ReviewFinalizedException(UUID reviewId) {
        super(String.format("Review '%s' is finalized and cannot be modified or deleted.", reviewId));
    }
}
