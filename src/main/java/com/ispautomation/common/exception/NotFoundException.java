package com.ispautomation.common.exception;

/**
 * Thrown when a requested resource cannot be found (HTTP 404).
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(404, message);
    }
}