package com.hr.performancepulse.exception;

/**
 * Thrown when an illegal ReviewCycle state transition is attempted
 * (e.g. activating an already-active cycle, or closing a closed cycle).
 *
 * <p>LLD §10 – maps to HTTP 422 / INVALID_CYCLE_STATE.
 * LLD §11 – enforced in CycleServiceImpl.activateCycle() / closeCycle().
 */
public class InvalidCycleStateException extends RuntimeException {
    public InvalidCycleStateException(String message) {
        super(message);
    }

    public InvalidCycleStateException(String currentState, String targetState) {
        super(String.format("Cannot transition cycle from '%s' to '%s'.", currentState, targetState));
    }
}
