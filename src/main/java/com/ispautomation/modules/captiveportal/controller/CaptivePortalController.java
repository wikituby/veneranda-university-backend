package com.ispautomation.modules.captiveportal.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Public Captive Portal controller — no authentication required.
 *
 * Exposes available packages and simulates mobile money payment flow
 * for hotspot/guest WiFi access.
 */
@Path("/api/v1/captive-portal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Captive Portal", description = "Public hotspot access — plan selection & mobile money payment")
public class CaptivePortalController {

    // In-memory session store (will be moved to DB in later phases)
    private static final List<Map<String, Object>> activeSessions = new ArrayList<>();

    @GET
    @Path("/packages")
    @Operation(summary = "Get available internet packages for guest users")
    public Response getPackages() {
        List<Map<String, Object>> packages = List.of(
            createPackage("1 Hour Access", 500, 60, "UGX", "bi-clock"),
            createPackage("3 Hours Access", 1200, 180, "UGX", "bi-clock-history"),
            createPackage("6 Hours Access", 2000, 360, "UGX", "bi-stopwatch"),
            createPackage("12 Hours Access", 3500, 720, "UGX", "bi-brightness-high"),
            createPackage("24 Hours Access", 5000, 1440, "UGX", "bi-sun"),
            createPackage("7 Days Unlimited", 15000, 10080, "UGX", "bi-calendar-week")
        );
        return Response.ok(packages).build();
    }

    @POST
    @Path("/initiate-payment")
    @Operation(summary = "Initiate mobile money payment for a selected package")
    public Response initiatePayment(Map<String, Object> request) {
        String phoneNumber = (String) request.get("phoneNumber");
        String packageId = (String) request.get("packageId");

        if (phoneNumber == null || phoneNumber.isBlank()) {
            return Response.status(400).entity(Map.of("error", "Phone number is required")).build();
        }
        if (packageId == null || packageId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "Package selection is required")).build();
        }

        // Simulate: Send push notification to user's phone
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Payment request sent to " + phoneNumber);
        response.put("transactionId", UUID.randomUUID().toString());
        response.put("phoneNumber", phoneNumber);
        response.put("packageId", packageId);
        response.put("status", "PENDING_PIN");
        response.put("expiresAt", LocalDateTime.now().plusMinutes(2).toString());
        return Response.ok(response).build();
    }

    @POST
    @Path("/confirm-payment")
    @Operation(summary = "Confirm mobile money PIN and activate internet access")
    public Response confirmPayment(Map<String, Object> request) {
        String transactionId = (String) request.get("transactionId");
        String pin = (String) request.get("pin");
        String phoneNumber = (String) request.get("phoneNumber");

        if (pin == null || pin.isBlank()) {
            return Response.status(400).entity(Map.of("error", "PIN is required")).build();
        }

        // Simulate: Validate PIN (in production, this calls MTN/Airtel API)
        if (pin.length() < 4) {
            return Response.status(400).entity(Map.of("error", "Invalid PIN")).build();
        }

        // Simulate: Payment successful, create session
        String sessionId = UUID.randomUUID().toString();
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("sessionId", sessionId);
        session.put("phoneNumber", phoneNumber);
        session.put("transactionId", transactionId);
        session.put("status", "ACTIVE");
        session.put("startedAt", LocalDateTime.now().toString());
        session.put("expiresAt", LocalDateTime.now().plusMinutes(60).toString());
        session.put("dataUsed", "0 MB");
        session.put("timeRemaining", "60 minutes");
        activeSessions.add(session);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Payment confirmed! Internet access activated.");
        response.put("sessionId", sessionId);
        response.put("status", "ACTIVE");
        response.put("startedAt", session.get("startedAt"));
        response.put("expiresAt", session.get("expiresAt"));
        response.put("timeRemaining", session.get("timeRemaining"));
        response.put("dataUsed", session.get("dataUsed"));
        return Response.ok(response).build();
    }

    @GET
    @Path("/payment-status/{transactionId}")
    @Operation(summary = "Poll payment status (PENDING_PIN → COMPLETED/FAILED)")
    public Response getPaymentStatus(@PathParam("transactionId") String transactionId) {
        // Simulate: In production, this would query MTN/Airtel API
        // For demo: after a few seconds, auto-complete the payment
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("transactionId", transactionId);

        // Look for an already-completed session
        boolean completed = activeSessions.stream()
                .anyMatch(s -> transactionId.equals(s.get("transactionId")));
        
        if (completed) {
            response.put("status", "COMPLETED");
            response.put("message", "Payment confirmed on phone");
        } else {
            response.put("status", "PENDING_PIN");
            response.put("message", "Awaiting PIN entry on phone");
        }

        return Response.ok(response).build();
    }

    @GET
    @Path("/session/{sessionId}")
    @Operation(summary = "Get active session status")
    public Response getSession(@PathParam("sessionId") String sessionId) {
        Optional<Map<String, Object>> session = activeSessions.stream()
                .filter(s -> s.get("sessionId").equals(sessionId))
                .findFirst();

        if (session.isEmpty()) {
            return Response.status(404).entity(Map.of("error", "Session not found")).build();
        }

        Map<String, Object> s = session.get();
        if ("EXPIRED".equals(s.get("status"))) {
            return Response.ok(s).build();
        }

        // Calculate remaining time
        LocalDateTime expiresAt = LocalDateTime.parse((String) s.get("expiresAt"));
        long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
        if (remainingMinutes <= 0) {
            s.put("status", "EXPIRED");
            s.put("timeRemaining", "Expired");
        } else {
            s.put("timeRemaining", remainingMinutes + " minutes");
        }

        return Response.ok(s).build();
    }

    @POST
    @Path("/logout")
    @Operation(summary = "End an active session")
    public Response logout(Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        activeSessions.removeIf(s -> s.get("sessionId").equals(sessionId));
        return Response.ok(Map.of("success", true, "message", "Session ended")).build();
    }

    private Map<String, Object> createPackage(String name, int price, int durationMinutes, String currency, String icon) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("id", UUID.randomUUID().toString().substring(0, 8));
        pkg.put("name", name);
        pkg.put("price", price);
        pkg.put("currency", currency);
        pkg.put("durationMinutes", durationMinutes);
        pkg.put("durationLabel", formatDuration(durationMinutes));
        pkg.put("icon", icon);
        return pkg;
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " minutes";
        int hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "");
        int days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "");
    }
}