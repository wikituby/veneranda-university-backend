package com.ispautomation.modules.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.modules.course.dto.CourseSubscriptionDto;
import com.ispautomation.modules.payment.service.FlutterwavePaymentService;
import com.ispautomation.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/api/v1/payments/flutterwave")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Flutterwave", description = "Flutterwave payment verify and webhook")
public class FlutterwaveController {

    private static final Logger LOG = Logger.getLogger(FlutterwaveController.class);

    @Inject
    FlutterwavePaymentService flutterwavePaymentService;

    @Inject
    SecurityContext securityContext;

    @GET
    @Path("/verify")
    @Operation(summary = "Verify Flutterwave payment by tx_ref and unlock subscription")
    public Response verify(@QueryParam("tx_ref") String txRef) {
        securityContext.requireAuthenticated();
        CourseSubscriptionDto subscription = flutterwavePaymentService.verifyAndComplete(
                txRef,
                securityContext.getUserId()
        );
        return Response.ok(subscription).build();
    }

    @POST
    @Path("/webhook")
    @Operation(summary = "Flutterwave webhook (charge.completed)")
    public Response webhook(
            @HeaderParam("verif-hash") String verifHash,
            JsonNode payload
    ) {
        if (!flutterwavePaymentService.validateWebhookSignature(verifHash)) {
            LOG.warn("Rejected Flutterwave webhook: invalid verif-hash");
            throw new BusinessException(401, "Invalid webhook signature.");
        }
        try {
            flutterwavePaymentService.handleWebhook(payload);
        } catch (BusinessException ex) {
            // Acknowledge so Flutterwave does not retry endlessly on business failures we logged
            LOG.warnf("Flutterwave webhook handled with business error: %s", ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Flutterwave webhook processing failed", ex);
            return Response.serverError().entity("{\"ok\":false}").build();
        }
        return Response.ok("{\"ok\":true}").type(MediaType.APPLICATION_JSON).build();
    }
}
