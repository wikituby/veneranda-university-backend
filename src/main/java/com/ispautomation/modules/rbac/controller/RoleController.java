package com.ispautomation.modules.rbac.controller;

import com.ispautomation.common.pagination.PageRequest;
import com.ispautomation.common.pagination.PageResponse;
import com.ispautomation.modules.rbac.dto.RoleDto;
import com.ispautomation.modules.rbac.service.RbacService;
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

import java.util.Set;

/**
 * Role management REST endpoints (RBAC).
 */
@Path("/api/v1/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Role Management", description = "CRUD operations for roles and role-permission assignments")
public class RoleController {

    @Inject
    RbacService rbacService;

    @Inject
    SecurityContext securityContext;

    @GET
    @Operation(summary = "List roles", description = "Paginated, searchable list of roles")
    public Response listRoles(
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("20") Integer size,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDir") @DefaultValue("asc") String sortDir,
            @QueryParam("search") String search) {

        securityContext.requireAuthenticated();
        securityContext.requirePermission("role:read");

        PageRequest pageRequest = PageRequest.of(page, size, sortBy, sortDir, search);
        PageResponse<RoleDto> result = rbacService.listRoles(pageRequest, securityContext.getTenantId());
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get role by id")
    public Response getRole(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("role:read");
        return Response.ok(rbacService.getRoleById(id)).build();
    }

    @POST
    @Operation(summary = "Create role")
    public Response createRole(@Valid CreateRoleRequest request) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("role:manage");
        RoleDto created = rbacService.createRole(
                request.getName(), request.getCode(), request.getDescription(),
                securityContext.getTenantId(), request.getPermissionIds(), securityContext.getUserId());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update role")
    public Response updateRole(@PathParam("id") Long id, @Valid UpdateRoleRequest request) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("role:manage");
        RoleDto updated = rbacService.updateRole(
                id, request.getName(), request.getDescription(),
                request.getIsActive(), request.getPermissionIds(), securityContext.getUserId());
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete role")
    public Response deleteRole(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("role:manage");
        rbacService.deleteRole(id);
        return Response.noContent().build();
    }

    // ===== Inner request DTOs =====

    public static class CreateRoleRequest {
        @jakarta.validation.constraints.NotBlank(message = "Name is required")
        @jakarta.validation.constraints.Size(max = 50, message = "Name must not exceed 50 characters")
        private String name;

        @jakarta.validation.constraints.NotBlank(message = "Code is required")
        @jakarta.validation.constraints.Size(max = 50, message = "Code must not exceed 50 characters")
        private String code;

        private String description;
        private Set<Long> permissionIds;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Set<Long> getPermissionIds() { return permissionIds; }
        public void setPermissionIds(Set<Long> permissionIds) { this.permissionIds = permissionIds; }
    }

    public static class UpdateRoleRequest {
        @jakarta.validation.constraints.Size(max = 50, message = "Name must not exceed 50 characters")
        private String name;
        private String description;
        private Boolean isActive;
        private Set<Long> permissionIds;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Set<Long> getPermissionIds() { return permissionIds; }
        public void setPermissionIds(Set<Long> permissionIds) { this.permissionIds = permissionIds; }
    }
}