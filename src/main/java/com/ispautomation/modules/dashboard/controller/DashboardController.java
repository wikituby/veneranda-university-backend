package com.ispautomation.modules.dashboard.controller;

import com.ispautomation.modules.rbac.repository.UserRepository;
import com.ispautomation.modules.router.service.RouterService;
import com.ispautomation.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard REST endpoint providing KPI summaries.
 * Currently returns foundation-level metrics; will be expanded as modules are built.
 */
@Path("/api/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Dashboard", description = "KPI summaries and dashboard metrics")
public class DashboardController {

    @Inject
    UserRepository userRepository;

    @Inject
    RouterService routerService;

    @Inject
    SecurityContext securityContext;

    @GET
    @Path("/summary")
    @Operation(summary = "Dashboard summary", description = "Returns high-level KPI counts for the dashboard")
    public Response summary() {
        securityContext.requireAuthenticated();

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.count("isActive = true");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", totalUsers);
        summary.put("activeUsers", activeUsers);
        // Placeholder KPIs - will be populated as modules are implemented
        summary.put("totalCustomers", 0);
        summary.put("activeSubscriptions", 0);
        // Fetch actual router statistics
        var routerStats = routerService.getDashboardStats(securityContext.getTenantId());
        summary.put("totalRouters", routerStats.get("totalRouters"));
        summary.put("onlineRouters", routerStats.get("onlineRouters"));
        summary.put("onlineHotspotUsers", 0);
        summary.put("pendingInvoices", 0);
        summary.put("overdueInvoices", 0);
        summary.put("monthlyRevenue", 0);
        summary.put("openTickets", 0);
        summary.put("activeVouchers", 0);
        summary.put("onlineSessions", 0);

        return Response.ok(summary).build();
    }

    @GET
    @Path("/charts/revenue")
    @Operation(summary = "Revenue chart data", description = "Returns monthly revenue data for charts (placeholder)")
    public Response revenueChart() {
        securityContext.requireAuthenticated();

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", java.util.List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));
        chartData.put("data", java.util.List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        chartData.put("label", "Monthly Revenue (UGX)");

        return Response.ok(chartData).build();
    }

    @GET
    @Path("/charts/customer-growth")
    @Operation(summary = "Customer growth chart", description = "Returns customer growth data for charts (placeholder)")
    public Response customerGrowthChart() {
        securityContext.requireAuthenticated();

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("labels", java.util.List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun"));
        chartData.put("data", java.util.List.of(0, 0, 0, 0, 0, 0));
        chartData.put("label", "New Customers per Month");

        return Response.ok(chartData).build();
    }
}