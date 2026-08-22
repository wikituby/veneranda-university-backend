package com.ispautomation.common.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler that maps all exceptions to a consistent
 * {@link ErrorResponse} JSON payload.
 */
@Provider
public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all custom business exceptions.
     */
    @Provider
    public static class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {
        @Context
        UriInfo uriInfo;

        @Override
        public Response toResponse(BusinessException ex) {
            String path = uriInfo != null ? uriInfo.getPath() : null;
            LOG.warnf("Business exception [%d] at %s: %s", ex.getStatusCode(), path, ex.getMessage());
            ErrorResponse body = new ErrorResponse(
                    ex.getStatusCode(),
                    Response.Status.fromStatusCode(ex.getStatusCode()).getReasonPhrase(),
                    ex.getMessage(),
                    path,
                    null
            );
            return Response.status(ex.getStatusCode())
                    .entity(body)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Handles Jakarta Validation constraint violations (HTTP 400).
     */
    @Provider
    public static class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
        @Context
        UriInfo uriInfo;

        @Override
        public Response toResponse(ConstraintViolationException ex) {
            String path = uriInfo != null ? uriInfo.getPath() : null;
            List<String> details = ex.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.toList());
            LOG.warnf("Validation error at %s: %s", path, details);
            ErrorResponse body = new ErrorResponse(
                    400, "Bad Request", "Validation failed", path, details);
            return Response.status(400)
                    .entity(body)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Handles JAX-RS 404 (no matching route).
     */
    @Provider
    public static class JaxrsNotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
        @Context
        UriInfo uriInfo;

        @Override
        public Response toResponse(NotFoundException ex) {
            String path = uriInfo != null ? uriInfo.getPath() : null;
            ErrorResponse body = new ErrorResponse(404, "Not Found", "Resource not found", path, null);
            return Response.status(404)
                    .entity(body)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Handles HTTP 405 method not allowed.
     */
    @Provider
    public static class NotAllowedExceptionMapper implements ExceptionMapper<NotAllowedException> {
        @Context
        UriInfo uriInfo;

        @Override
        public Response toResponse(NotAllowedException ex) {
            String path = uriInfo != null ? uriInfo.getPath() : null;
            ErrorResponse body = new ErrorResponse(405, "Method Not Allowed", ex.getMessage(), path, null);
            return Response.status(405)
                    .entity(body)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Catch-all for unexpected exceptions (HTTP 500).
     */
    @Provider
    public static class GenericExceptionMapper implements ExceptionMapper<Throwable> {
        @Context
        UriInfo uriInfo;

        @Override
        public Response toResponse(Throwable ex) {
            String path = uriInfo != null ? uriInfo.getPath() : null;
            LOG.errorf(ex, "Unexpected error at %s", path);
            ErrorResponse body = new ErrorResponse(
                    500, "Internal Server Error", "An unexpected error occurred", path, null);
            return Response.status(500)
                    .entity(body)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}