package com.ispautomation.modules.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ispautomation.common.exception.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Verifies Google Identity Services ID tokens via Google's tokeninfo endpoint.
 */
@ApplicationScoped
public class GoogleIdTokenVerifier {

    private static final Logger LOG = Logger.getLogger(GoogleIdTokenVerifier.class);
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    @ConfigProperty(name = "app.google.client-id")
    Optional<String> clientId;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public GoogleIdentity verify(String idToken) {
        if (clientId.isEmpty() || clientId.get().isBlank()) {
            throw new UnauthorizedException("Google Sign-In is not configured on the server");
        }
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Missing Google ID token");
        }

        try {
            String encoded = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKENINFO_URL + encoded))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warnf("Google tokeninfo returned status %s", response.statusCode());
                throw new UnauthorizedException("Invalid Google token");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String aud = text(json, "aud");
            if (!clientId.get().equals(aud)) {
                throw new UnauthorizedException("Google token audience mismatch");
            }

            String email = text(json, "email");
            if (email == null || email.isBlank()) {
                throw new UnauthorizedException("Google account email is required");
            }

            String emailVerified = text(json, "email_verified");
            if (!"true".equalsIgnoreCase(emailVerified)) {
                throw new UnauthorizedException("Google email is not verified");
            }

            String sub = text(json, "sub");
            if (sub == null || sub.isBlank()) {
                throw new UnauthorizedException("Invalid Google subject");
            }

            return new GoogleIdentity(
                    sub,
                    email.trim().toLowerCase(),
                    text(json, "given_name"),
                    text(json, "family_name"),
                    text(json, "name"),
                    text(json, "picture")
            );
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Failed to verify Google ID token", ex);
            throw new UnauthorizedException("Could not verify Google token");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public record GoogleIdentity(
            String sub,
            String email,
            String givenName,
            String familyName,
            String fullName,
            String pictureUrl
    ) {
    }
}
