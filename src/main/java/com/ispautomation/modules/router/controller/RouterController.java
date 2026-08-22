package com.ispautomation.modules.router.controller;

import com.ispautomation.common.pagination.PageRequest;
import com.ispautomation.common.pagination.PageResponse;
import com.ispautomation.modules.router.dto.CreateRouterRequest;
import com.ispautomation.modules.router.dto.RouterDto;
import com.ispautomation.modules.router.dto.UpdateRouterRequest;
import com.ispautomation.modules.router.service.RouterService;
import com.ispautomation.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Router management REST endpoints.
 */
@Path("/api/v1/routers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Router Management", description = "CRUD operations for network routers/devices")
public class RouterController {

    @Inject
    RouterService routerService;

    @Inject
    SecurityContext securityContext;

    @GET
    @Operation(summary = "List routers", description = "Paginated, searchable list of routers")
    public Response listRouters(
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("20") Integer size,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDir") @DefaultValue("asc") String sortDir,
            @QueryParam("search") String search) {

        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:read");

        PageRequest pageRequest = PageRequest.of(page, size, sortBy, sortDir, search);
        PageResponse<RouterDto> result = routerService.listRouters(pageRequest, securityContext.getTenantId());
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get router by id")
    public Response getRouter(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:read");
        return Response.ok(routerService.getRouterById(id)).build();
    }

    @POST
    @Operation(summary = "Create router")
    public Response createRouter(@Valid CreateRouterRequest request) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:manage");
        RouterDto created = routerService.createRouter(request, securityContext.getTenantId(), securityContext.getUserId());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update router")
    public Response updateRouter(@PathParam("id") Long id, @Valid UpdateRouterRequest request) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:manage");
        RouterDto updated = routerService.updateRouter(id, request, securityContext.getUserId());
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete router")
    public Response deleteRouter(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:manage");
        routerService.deleteRouter(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/enable")
    @Operation(summary = "Enable router")
    public Response enableRouter(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:manage");
        RouterDto result = routerService.enableRouter(id, securityContext.getUserId());
        return Response.ok(result).build();
    }

    @POST
    @Path("/{id}/disable")
    @Operation(summary = "Disable router")
    public Response disableRouter(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:manage");
        RouterDto result = routerService.disableRouter(id, securityContext.getUserId());
        return Response.ok(result).build();
    }

    @POST
    @Path("/{id}/test-connection")
    @Operation(summary = "Test connectivity to a router")
    public Response testConnection(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:manage");
        RouterDto result = routerService.testConnection(id, securityContext.getUserId());
        return Response.ok(result).build();
    }

    @POST
    @Path("/{id}/sync")
    @Operation(summary = "Synchronize router information")
    public Response synchronizeRouter(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:manage");
        RouterDto result = routerService.synchronizeRouter(id, securityContext.getUserId());
        return Response.ok(result).build();
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Get router dashboard statistics")
    public Response getStats() {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("router:read");
        return Response.ok(routerService.getDashboardStats(securityContext.getTenantId())).build();
    }
}