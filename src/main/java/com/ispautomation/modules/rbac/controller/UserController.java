package com.ispautomation.modules.rbac.controller;

import com.ispautomation.common.pagination.PageRequest;
import com.ispautomation.common.pagination.PageResponse;
import com.ispautomation.modules.rbac.dto.CreateUserRequest;
import com.ispautomation.modules.rbac.dto.UpdateUserRequest;
import com.ispautomation.modules.rbac.dto.UserDto;
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

/**
 * User management REST endpoints (RBAC).
 */
@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Management", description = "CRUD operations for system users")
public class UserController {

    @Inject
    RbacService rbacService;

    @Inject
    SecurityContext securityContext;

    @GET
    @Operation(summary = "List users", description = "Paginated, searchable list of users")
    public Response listUsers(
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("20") Integer size,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDir") @DefaultValue("asc") String sortDir,
            @QueryParam("search") String search) {

        securityContext.requireAuthenticated();
        securityContext.requirePermission("user:read");

        PageRequest pageRequest = PageRequest.of(page, size, sortBy, sortDir, search);
        PageResponse<UserDto> result = rbacService.listUsers(pageRequest, securityContext.getTenantId());
        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get user by id")
    public Response getUser(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("user:read");
        return Response.ok(rbacService.getUserById(id)).build();
    }

    @POST
    @Operation(summary = "Create user")
    public Response createUser(@Valid CreateUserRequest request) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("user:create");
        UserDto created = rbacService.createUser(request, securityContext.getTenantId(), securityContext.getUserId());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update user")
    public Response updateUser(@PathParam("id") Long id, @Valid UpdateUserRequest request) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("user:update");
        UserDto updated = rbacService.updateUser(id, request, securityContext.getUserId());
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete user")
    public Response deleteUser(@PathParam("id") Long id) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("user:delete");
        rbacService.deleteUser(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/reset-password")
    @Operation(summary = "Reset user password")
    public Response resetPassword(@PathParam("id") Long id, @Valid PasswordResetRequest request) {
        securityContext.requireAuthenticated();
        securityContext.requirePermission("user:update");
        rbacService.resetPassword(id, request.getNewPassword());
        return Response.noContent().build();
    }

    /**
     * Inner DTO for password reset.
     */
    public static class PasswordResetRequest {
        @jakarta.validation.constraints.NotBlank(message = "New password is required")
        @jakarta.validation.constraints.Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        private String newPassword;

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}