package com.ispautomation.common.exception;

/**
 * Thrown when a request conflicts with the current state of a resource (HTTP 409).
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(409, message);
    }
}