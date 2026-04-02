package com.hr.performancepulse.exception;

/**
 * Thrown when attempting to create an employee with an email that already exists.
 *
 * <p>LLD §10 – maps to HTTP 409 CONFLICT / DUPLICATE_EMPLOYEE.
 * Enforced in EmployeeServiceImpl.createEmployee().
 */
public class DuplicateEmployeeException extends RuntimeException {
    public DuplicateEmployeeException(String email) {
        super(String.format("Employee with email '%s' already exists", email));
    }
}
