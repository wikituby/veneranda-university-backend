package com.ispautomation.security;


import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    @Inject
    SecurityContext securityContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {

        // Ignore CORS preflight
        if ("OPTIONS".equalsIgnoreCase(
                requestContext.getMethod())) {

            return;
        }



        String path =
                requestContext
                .getUriInfo()
                .getPath();



        // Public authentication endpoints
        if(path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/google")
                || path.startsWith("/api/v1/auth/register")
                || path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/payments/flutterwave/webhook")
                || path.startsWith("/api/v1/settings/public")) {

            return;
        }




        String authHeader =
                requestContext
                .getHeaderString(
                        HttpHeaders.AUTHORIZATION);



        securityContext.authenticate(authHeader);

    }

}