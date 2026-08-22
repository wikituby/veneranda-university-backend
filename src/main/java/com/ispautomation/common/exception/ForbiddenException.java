package com.ispautomation.common.exception;

/**
 * Thrown when the authenticated user lacks permission for a resource (HTTP 403).
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(403, message);
    }
}