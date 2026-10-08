package com.commercehub.order.infrastructure.client;

import com.commercehub.order.exception.RemoteProductServiceException;
import com.commercehub.order.exception.UnknownProductException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * Maps Product Service HTTP errors: 404 becomes a business unknown-product error;
 * other failures become a remote-service error (eligible for retry / circuit breaker).
 */
public class ProductClientExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    @Override
    public RuntimeException toThrowable(Response response) {
        int status = response.getStatus();
        if (status == 404) {
            return new UnknownProductException("requested");
        }
        return new RemoteProductServiceException("Product Service returned HTTP " + status);
    }

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400;
    }
}
