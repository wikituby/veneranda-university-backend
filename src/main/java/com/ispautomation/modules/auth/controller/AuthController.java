package com.ispautomation.modules.auth.controller;

import com.ispautomation.modules.auth.dto.ChangePasswordRequest;
import com.ispautomation.modules.auth.dto.GoogleLoginRequest;
import com.ispautomation.modules.auth.dto.LoginRequest;
import com.ispautomation.modules.auth.dto.RegisterRequest;
import com.ispautomation.modules.auth.dto.RefreshTokenRequest;
import com.ispautomation.modules.auth.dto.TokenResponse;
import com.ispautomation.modules.auth.dto.UpdateProfileRequest;
import com.ispautomation.modules.auth.service.AuthService;
import com.ispautomation.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

/**
 * Authentication REST endpoints.
 */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Login, refresh, logout, and current user info")
public class AuthController {

    @Inject
    AuthService authService;

    @Inject
    SecurityContext securityContext;

    @POST
    @Path("/login")
    @Operation(summary = "Login", description = "Authenticate a user and receive access + refresh tokens")
    public Response login(@Valid LoginRequest request,
                          @Context HttpHeaders headers,
                          @Context UriInfo uriInfo) {
        String ipAddress = extractClientIp(headers);
        String userAgent = headers.getHeaderString("User-Agent");
        TokenResponse tokens = authService.login(request, ipAddress, userAgent);
        return Response.ok(tokens).build();
    }

    @POST
    @Path("/google")
    @Operation(summary = "Google Sign-In", description = "Authenticate or register with a Google ID token")
    public Response googleLogin(@Valid GoogleLoginRequest request,
                                @Context HttpHeaders headers) {
        String ipAddress = extractClientIp(headers);
        String userAgent = headers.getHeaderString("User-Agent");
        TokenResponse tokens = authService.loginWithGoogle(request.getIdToken(), ipAddress, userAgent);
        return Response.ok(tokens).build();
    }

    @POST
    @Path("/register")
    @Operation(summary = "Register", description = "Create a student account and receive access + refresh tokens")
    public Response register(@Valid RegisterRequest request,
                             @Context HttpHeaders headers) {
        String ipAddress = extractClientIp(headers);
        String userAgent = headers.getHeaderString("User-Agent");
        TokenResponse tokens = authService.register(request, ipAddress, userAgent);
        return Response.status(Response.Status.CREATED).entity(tokens).build();
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh token", description = "Exchange a refresh token for a new access + refresh token pair")
    public Response refresh(@Valid RefreshTokenRequest request,
                            @Context HttpHeaders headers) {
        String ipAddress = extractClientIp(headers);
        String userAgent = headers.getHeaderString("User-Agent");
        TokenResponse tokens = authService.refresh(request, ipAddress, userAgent);
        return Response.ok(tokens).build();
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Logout", description = "Revoke all refresh tokens for the current user")
    public Response logout(@Context HttpHeaders headers) {
        securityContext.requireAuthenticated();
        String ipAddress = extractClientIp(headers);
        String userAgent = headers.getHeaderString("User-Agent");
        authService.logout(securityContext.getUserId(), ipAddress, userAgent);
        return Response.noContent().build();
    }

    @GET
    @Path("/me")
    @Operation(summary = "Current user", description = "Get the authenticated user's profile and permissions")
    public Response me() {
        securityContext.requireAuthenticated();
        TokenResponse.UserInfo userInfo = authService.getCurrentUserInfo(securityContext.getUserId());
        return Response.ok(userInfo).build();
    }

    @PUT
    @Path("/me")
    @Operation(summary = "Update profile", description = "Update the authenticated user's name, email, and phone")
    public Response updateProfile(@Valid UpdateProfileRequest request) {
        securityContext.requireAuthenticated();
        TokenResponse.UserInfo userInfo = authService.updateProfile(securityContext.getUserId(), request);
        return Response.ok(userInfo).build();
    }

    @PUT
    @Path("/me/password")
    @Operation(summary = "Change password", description = "Change or set the authenticated user's password")
    public Response changePassword(@Valid ChangePasswordRequest request) {
        securityContext.requireAuthenticated();
        TokenResponse.UserInfo userInfo = authService.changePassword(securityContext.getUserId(), request);
        return Response.ok(userInfo).build();
    }

    @POST
    @Path("/me/avatar/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload profile photo", description = "Upload a profile photo for the authenticated user")
    public Response uploadAvatar(@RestForm("file") FileUpload file) throws Exception {
        securityContext.requireAuthenticated();
        if (file == null || file.size() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Profile photo file is required"))
                    .build();
        }

        try (InputStream data = Files.newInputStream(file.uploadedFile())) {
            TokenResponse.UserInfo userInfo = authService.uploadAvatar(
                    securityContext.getUserId(),
                    file.fileName(),
                    file.contentType(),
                    file.size(),
                    data
            );
            return Response.status(Response.Status.CREATED).entity(userInfo).build();
        }
    }

    private String extractClientIp(HttpHeaders headers) {
        String xff = headers.getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return headers.getHeaderString("X-Real-IP");
    }
}
