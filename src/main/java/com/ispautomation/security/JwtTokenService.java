package com.ispautomation.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import java.time.Duration;
import java.time.Instant;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;



@ApplicationScoped
public class JwtTokenService {


    @ConfigProperty(
            name = "app.jwt.issuer",
            defaultValue = "ispautomation"
    )
    String issuer;



    @ConfigProperty(
            name = "app.jwt.audience",
            defaultValue = "ispautomation-users"
    )
    String audience;



    @ConfigProperty(
            name = "app.jwt.access-token-ttl",
            defaultValue = "900"
    )
    long accessTokenTtlSeconds;



    @ConfigProperty(
            name = "app.jwt.refresh-token-ttl",
            defaultValue = "604800"
    )
    long refreshTokenTtlSeconds;



    @ConfigProperty(
            name = "app.jwt.secret"
    )
    String secret;



    private SecretKey signingKey;



    private SecretKey getSigningKey() {


        if(signingKey == null){


            byte[] keyBytes =
                    secret.getBytes(StandardCharsets.UTF_8);



            if(keyBytes.length < 32){

                throw new IllegalArgumentException(
                        "JWT secret must be at least 32 characters"
                );
            }


            signingKey =
                    Keys.hmacShaKeyFor(keyBytes);

        }


        return signingKey;
    }





    public String generateAccessToken(
            Long userId,
            String username,
            Long tenantId,
            String branchId,
            Set<String> roles,
            Set<String> permissions
    ){


        Instant now = Instant.now();

        Instant expiry =
                now.plusSeconds(accessTokenTtlSeconds);



        return Jwts.builder()

                .issuer(issuer)

                .audience()
                .add(audience)
                .and()

                .subject(username)

                .id(String.valueOf(userId))

                .claim("userId", userId)

                .claim("tenantId", tenantId)

                .claim("branchId", branchId)

                .claim("roles", roles)

                .claim("permissions", permissions)

                .claim("type","ACCESS")

                .issuedAt(Date.from(now))

                .expiration(Date.from(expiry))

                .signWith(getSigningKey())

                .compact();

    }






    public String generateRefreshToken(
            Long userId,
            String username
    ){


        Instant now = Instant.now();


        Instant expiry =
                now.plusSeconds(refreshTokenTtlSeconds);



        return Jwts.builder()

                .issuer(issuer)

                .audience()
                .add(audience)
                .and()

                .subject(username)

                .id(UUID.randomUUID().toString())

                .claim("userId", userId)

                .claim("type","REFRESH")

                .issuedAt(Date.from(now))

                .expiration(Date.from(expiry))

                .signWith(getSigningKey())

                .compact();

    }







    public Jws<Claims> parseToken(String token){


        return Jwts.parser()

                .verifyWith(getSigningKey())

                .requireIssuer(issuer)

                .build()

                .parseSignedClaims(token);

    }







    public Long extractUserId(String token){


        Claims claims =
                parseToken(token).getPayload();



        Object id =
                claims.get("userId");



        if(id instanceof Number number){

            return number.longValue();

        }


        return Long.valueOf(id.toString());

    }







    @SuppressWarnings("unchecked")
    public Set<String> extractPermissions(String token){


        Claims claims =
                parseToken(token).getPayload();



        Object value =
                claims.get("permissions");



        if(value instanceof List<?> list){


            return new HashSet<>(
                    (List<String>) list
            );

        }


        return Set.of();

    }





    public Duration getAccessTokenTtl(){

        return Duration.ofSeconds(accessTokenTtlSeconds);

    }





    public Duration getRefreshTokenTtl(){

        return Duration.ofSeconds(refreshTokenTtlSeconds);

    }

}