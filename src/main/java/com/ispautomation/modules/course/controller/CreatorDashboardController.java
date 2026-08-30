package com.ispautomation.modules.course.controller;

import com.ispautomation.modules.course.dto.CreatorDashboardDto;
import com.ispautomation.modules.course.service.CreatorDashboardService;
import com.ispautomation.security.SecurityContext;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Dedicated creator analytics endpoint (avoids path clashes under course-categories).
 */
@Path("/api/v1/creator-dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Creator Dashboard", description = "Programme creator analytics and join requests")
public class CreatorDashboardController {

    @Inject
    CreatorDashboardService creatorDashboardService;

    @Inject
    SecurityContext securityContext;

    @GET
    @Operation(summary = "Creator programme dashboard", description = "Stats, charts, and pending join requests for programmes you created")
    public Response getDashboard(@QueryParam("programmeId") String programmeId) {
        securityContext.requireAuthenticated();
        CreatorDashboardDto dashboard = creatorDashboardService.getDashboard(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                programmeId
        );
        return Response.ok(dashboard).build();
    }
}