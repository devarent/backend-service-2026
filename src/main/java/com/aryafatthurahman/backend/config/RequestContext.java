package com.aryafatthurahman.backend.config;

import jakarta.enterprise.context.RequestScoped;

/**
 * Per-request metadata used by the correlation-id filter and the error envelope.
 */
@RequestScoped
public class RequestContext {

    private String requestId;
    private String path;
    private String method;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
