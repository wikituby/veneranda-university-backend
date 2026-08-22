package com.ispautomation.common.exception;

/**
 * Thrown when authentication is required or has failed (HTTP 401).
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(401, message);
    }
}