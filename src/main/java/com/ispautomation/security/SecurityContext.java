package com.ispautomation.security;


import com.ispautomation.common.exception.UnauthorizedException;
import com.ispautomation.modules.rbac.entity.Permission;
import com.ispautomation.modules.rbac.entity.Role;
import com.ispautomation.modules.rbac.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import jakarta.enterprise.context.RequestScoped;

import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;



@RequestScoped
public class SecurityContext {


    private static final Logger LOG =
            Logger.getLogger(SecurityContext.class);


    private static final String BEARER_PREFIX = "Bearer ";


    private final JwtTokenService jwtTokenService;



    private Long userId;

    private String username;

    private Long tenantId;

    private Long branchId;



    private Set<String> roles =
            new HashSet<>();


    private Set<String> permissions =
            new HashSet<>();


    private boolean authenticated = false;




    public SecurityContext(JwtTokenService jwtTokenService) {

        this.jwtTokenService = jwtTokenService;

    }






    public void authenticate(String authHeader) {


        reset();



        if(authHeader == null || authHeader.isBlank()) {


            LOG.warn("Authorization header is missing");

            return;

        }




        if(!authHeader.startsWith(BEARER_PREFIX)) {


            LOG.warn("Authorization header is not Bearer token");

            return;

        }




        String token =
                authHeader.substring(
                        BEARER_PREFIX.length()
                ).trim();




        try {



            Jws<Claims> parsed =
                    jwtTokenService.parseToken(token);



            Claims claims =
                    parsed.getPayload();




            LOG.debugf(
                    "JWT claims received: %s",
                    claims
            );





            this.username =
                    claims.getSubject();





            this.userId =
                    convertLong(
                            claims.get("userId")
                    );




            this.tenantId =
                    convertLong(
                            claims.get("tenantId")
                    );




            this.branchId =
                    convertLong(
                            claims.get("branchId")
                    );






            Object roleObject =
                    claims.get("roles");



            if(roleObject instanceof Iterable<?> iterable) {


                for(Object role : iterable) {


                    if(role != null) {

                        roles.add(
                                role.toString()
                        );

                    }

                }

            }






            permissions =
                    jwtTokenService.extractPermissions(token);






            authenticated = true;





            LOG.infof(
                    "USER AUTHENTICATED: username=%s userId=%s tenant=%s branch=%s roles=%s permissions=%d",
                    username,
                    userId,
                    tenantId,
                    branchId,
                    roles,
                    permissions.size()
            );




        }
        catch(Exception e) {


            LOG.error(
                    "JWT authentication failed",
                    e
            );


            authenticated=false;


        }


    }








    private Long convertLong(Object value) {


        if(value == null) {

            return null;

        }



        if(value instanceof Number number) {


            return number.longValue();

        }




        try {


            return Long.parseLong(
                    value.toString()
            );


        }
        catch(Exception e) {


            return null;

        }


    }








    private void reset() {


        userId = null;

        username = null;

        tenantId = null;

        branchId = null;


        roles.clear();

        permissions.clear();


        authenticated = false;


    }









    public void fromUser(User user) {


        reset();



        this.userId =
                user.getId();



        this.username =
                user.getUsername();




        this.tenantId =
                user.getTenant() != null
                        ? user.getTenant().getId()
                        : null;




        this.branchId =
                user.getBranch() != null
                        ? user.getBranch().getId()
                        : null;





        this.roles =
                user.getRoles()
                        .stream()
                        .map(Role::getCode)
                        .collect(Collectors.toSet());





        this.permissions =
                user.getRoles()
                        .stream()
                        .flatMap(
                                role ->
                                        role.getPermissions()
                                                .stream()
                        )
                        .map(Permission::getCode)
                        .collect(Collectors.toSet());




        authenticated=true;



    }








    public boolean isAuthenticated() {

        return authenticated;

    }






    public Long getUserId() {

        return userId;

    }






    public String getUsername() {

        return username;

    }






    public Long getTenantId() {

        return tenantId;

    }






    public Long getBranchId() {

        return branchId;

    }






    public Set<String> getRoles() {

        return roles;

    }






    public Set<String> getPermissions() {

        return permissions;

    }







    public boolean hasPermission(String permission) {


        return authenticated &&
                permissions.contains(permission);


    }







    public boolean hasAnyPermission(String... permissions) {


        if(!authenticated) {

            return false;

        }



        for(String permission : permissions) {


            if(this.permissions.contains(permission)) {

                return true;

            }


        }



        return false;


    }







    public boolean hasRole(String role) {


        return authenticated &&
                roles.contains(role);


    }







    public void requirePermission(String permission) {


        if(!hasPermission(permission)) {


            throw new com.ispautomation.common.exception.ForbiddenException(
                    "Access denied: missing permission '" + permission + "'"
            );


        }


    }








    public void requireAuthenticated() {


        if(!authenticated) {


            throw new UnauthorizedException(
                    "Authentication required"
            );


        }


    }


}