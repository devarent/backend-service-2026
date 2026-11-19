package com.aryafatthurahman.backend.config;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Accepts a caller-supplied correlation id when it is safe, otherwise generates one.
 * The resolved value is returned on every response and included in error payloads.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{8,128}");

    private final RequestContext requestContext;

    @Inject
    public RequestIdFilter(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    public void filter(ContainerRequestContext request) {
        String candidate = request.getHeaderString(HEADER_NAME);
        String requestId = candidate != null && SAFE_ID.matcher(candidate).matches()
                ? candidate
                : UUID.randomUUID().toString();

        requestContext.setRequestId(requestId);
        requestContext.setPath(request.getUriInfo().getRequestUri().getPath());
        requestContext.setMethod(request.getMethod());
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        if (requestContext.getRequestId() != null) {
            response.getHeaders().putSingle(HEADER_NAME, requestContext.getRequestId());
        }
    }
}
