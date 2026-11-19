package com.aryafatthurahman.backend.order.api;

import com.aryafatthurahman.backend.common.BusinessValidationException;
import com.aryafatthurahman.backend.common.ConflictException;
import com.aryafatthurahman.backend.common.ErrorResponse;
import com.aryafatthurahman.backend.common.FieldViolation;
import com.aryafatthurahman.backend.common.NotFoundException;
import com.aryafatthurahman.backend.config.RequestContext;
import com.aryafatthurahman.backend.config.RequestIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Translates every failure into the same JSON envelope, including a correlation
 * id that matches the X-Request-Id response header.
 */
@ApplicationScoped
public class ApiExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMappers.class);

    private final RequestContext requestContext;

    @Inject
    public ApiExceptionMappers(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @ServerExceptionMapper
    public Response mapValidation(ConstraintViolationException exception) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(this::toViolation)
                .distinct()
                .toList();
        return error(400, "VALIDATION_ERROR", "Request validation failed", violations);
    }

    @ServerExceptionMapper
    public Response mapBusinessValidation(BusinessValidationException exception) {
        return error(400, exception.getCode(), exception.getMessage(), List.of());
    }

    @ServerExceptionMapper
    public Response mapNotFound(NotFoundException exception) {
        return error(404, "NOT_FOUND", exception.getMessage(), List.of());
    }

    @ServerExceptionMapper
    public Response mapConflict(ConflictException exception) {
        return error(409, exception.getCode(), exception.getMessage(), List.of());
    }

    @ServerExceptionMapper
    public Response mapOptimisticLock(OptimisticLockException exception) {
        LOG.warn("Optimistic locking conflict while updating an order");
        return error(409, "CONCURRENT_MODIFICATION",
                "The order was modified by another request. Reload it and retry.", List.of());
    }

    @ServerExceptionMapper
    public Response mapMalformedJson(JsonProcessingException exception) {
        return error(400, "MALFORMED_JSON", "Request body is not valid JSON", List.of());
    }

    @ServerExceptionMapper
    public Response mapWebApplication(WebApplicationException exception) {
        int status = exception.getResponse() != null
                ? exception.getResponse().getStatus()
                : 500;
        String code = switch (status) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "HTTP_ERROR";
        };
        String message = status == 404
                ? "The requested resource was not found"
                : reason(status);
        return error(status, code, message, List.of());
    }

    @ServerExceptionMapper
    public Response mapUnexpected(Exception exception) {
        String requestId = requestContext.getRequestId();
        LOG.error("Unhandled exception requestId=" + requestId, exception);
        return error(500, "INTERNAL_ERROR",
                "An unexpected error occurred. Quote the request id when contacting support.", List.of());
    }

    private FieldViolation toViolation(ConstraintViolation<?> violation) {
        return new FieldViolation(lastField(violation.getPropertyPath()), violation.getMessage());
    }

    private String lastField(Path path) {
        String field = "request";
        for (Path.Node node : path) {
            if (node.getName() != null) {
                field = node.getName();
            }
        }
        return field;
    }

    private Response error(int status, String code, String message, List<FieldViolation> violations) {
        String requestId = requestContext.getRequestId();
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        String path = requestContext.getPath() == null ? "unknown" : requestContext.getPath();

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status,
                reason(status),
                code,
                message,
                path,
                requestId,
                violations);

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .header(RequestIdFilter.HEADER_NAME, requestId)
                .entity(body)
                .build();
    }

    private String reason(int status) {
        Response.Status resolved = Response.Status.fromStatusCode(status);
        return resolved == null ? "Error" : resolved.getReasonPhrase();
    }
}
