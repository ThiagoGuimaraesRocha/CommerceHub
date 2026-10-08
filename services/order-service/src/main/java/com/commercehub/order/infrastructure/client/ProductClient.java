package com.commercehub.order.infrastructure.client;

import com.commercehub.order.exception.RemoteProductServiceException;
import com.commercehub.order.exception.UnknownProductException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.temporal.ChronoUnit;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/v1/products")
@RegisterRestClient(configKey = "product-service")
@RegisterProvider(ProductClientExceptionMapper.class)
@Produces(MediaType.APPLICATION_JSON)
public interface ProductClient {

    @GET
    @Path("/{id}")
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @Retry(
            maxRetries = 2,
            delay = 200,
            retryOn = {RemoteProductServiceException.class, jakarta.ws.rs.ProcessingException.class},
            abortOn = {UnknownProductException.class})
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            failureRatio = 0.5,
            delay = 5000,
            failOn = {RemoteProductServiceException.class, jakarta.ws.rs.ProcessingException.class},
            skipOn = {UnknownProductException.class})
    ProductSnapshotResponse getById(@PathParam("id") String id);
}
