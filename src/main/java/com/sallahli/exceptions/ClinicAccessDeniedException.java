package com.sallahli.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to access data from a clinic they don't belong to.
 * This is used for multi-tenancy security enforcement.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ClinicAccessDeniedException extends RuntimeException {

    public ClinicAccessDeniedException(String message) {
        super(message);
    }

    public ClinicAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}

