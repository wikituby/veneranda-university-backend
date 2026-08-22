package com.ispautomation.modules.rbac.controller;

import com.ispautomation.modules.rbac.dto.PermissionDto;
import com.ispautomation.modules.rbac.service.RbacService;
import com.ispautomation.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Permission listing REST endpoints (RBAC).
 * Permissions are system-defined (read-only); creation is via Flyway migrations.
 */
@Path("/api/v1/permissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Permissions", description = "List system permissions grouped by module")
public class PermissionController {

    @Inject
    RbacService rbacService;

    @Inject
    SecurityContext securityContext;

    @GET
    @Operation(summary = "List all permissions", description = "Returns all system permissions, optionally filtered by module")
    public Response listPermissions(@QueryParam("module") String module) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("role:read");

        List<PermissionDto> permissions;
        if (module != null && !module.isBlank()) {
            permissions = rbacService.listPermissionsByModule(module);
        } else {
            permissions = rbacService.listAllPermissions();
        }
        return Response.ok(permissions).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get permission by id")
    public Response getPermission(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("role:read");
        return Response.ok(rbacService.getPermissionById(id)).build();
    }
}