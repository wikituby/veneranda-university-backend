package com.ispautomation.modules.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Bridges LMS events to a Google Apps Script webhook for free Google Groups automation.
 * Expected webhook response: {"ok": true, "groupEmail": "..."}.
 */
@ApplicationScoped
public class GoogleGroupsSyncService {

    private static final Logger LOG = Logger.getLogger(GoogleGroupsSyncService.class);

    @ConfigProperty(name = "app.google-groups.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "app.google-groups.webhook-url")
    Optional<String> webhookUrl;

    @ConfigProperty(name = "app.google-groups.secret")
    Optional<String> secret;

    @ConfigProperty(name = "app.google-groups.auto-create-group-on-root", defaultValue = "false")
    boolean autoCreateGroupOnRoot;

    @ConfigProperty(name = "app.google-groups.default-domain", defaultValue = "googlegroups.com")
    String defaultDomain;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            // Apps Script returns 302; auto-follow may switch POST to GET and fail.
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public boolean isAutoCreateGroupOnRootEnabled() {
        return enabled && autoCreateGroupOnRoot && webhookUrl.isPresent() && secret.isPresent();
    }

    public String createGroupForCourse(String courseTitle) {
        if (!isAutoCreateGroupOnRootEnabled()) {
            return null;
        }

        String localPart = slug(courseTitle);
        String requestedEmail = localPart + "@" + defaultDomain;

        Map<String, Object> body = Map.of(
                "secret", secret.get(),
                "action", "create_group",
                "name", courseTitle,
                "email", requestedEmail
        );

        try {
            WebhookResponse response = post(body);
            if (Boolean.TRUE.equals(response.ok)) {
                return response.groupEmail != null && !response.groupEmail.isBlank()
                        ? response.groupEmail
                        : requestedEmail;
            }
            LOG.warnf("Google group create failed: %s", response.error);
        } catch (Exception ex) {
            LOG.error("Google group create webhook call failed", ex);
        }
        return null;
    }

    public boolean isMemberSyncConfigured() {
        return enabled && webhookUrl.isPresent() && secret.isPresent()
                && !webhookUrl.get().isBlank() && !secret.get().isBlank();
    }

    public void addMember(String groupEmail, String memberEmail) {
        syncMember("add", groupEmail, memberEmail);
    }

    public void removeMember(String groupEmail, String memberEmail) {
        syncMember("remove", groupEmail, memberEmail);
    }

    /** Attempts member sync and returns success/error for enrollment tracking. */
    public SyncResult tryAddMember(String groupEmail, String memberEmail) {
        return syncMember("add", groupEmail, memberEmail);
    }

    public SyncResult tryRemoveMember(String groupEmail, String memberEmail) {
        return syncMember("remove", groupEmail, memberEmail);
    }

    private SyncResult syncMember(String action, String groupEmail, String memberEmail) {
        if (!isMemberSyncConfigured()) {
            return SyncResult.skipped("Google Groups sync is not configured");
        }

        Map<String, Object> body = Map.of(
                "secret", secret.get(),
                "action", action,
                "group", groupEmail,
                "email", memberEmail
        );

        try {
            WebhookResponse response = post(body);
            if (Boolean.TRUE.equals(response.ok)) {
                return SyncResult.ok();
            }
            String error = response.error != null && !response.error.isBlank()
                    ? response.error
                    : "Google Groups webhook returned ok=false";
            LOG.warnf("Google group member sync failed [%s] %s -> %s: %s",
                    action, memberEmail, groupEmail, error);
            return SyncResult.failed(error);
        } catch (Exception ex) {
            LOG.errorf(ex, "Google group member sync webhook call failed [%s] %s -> %s",
                    action, memberEmail, groupEmail);
            return SyncResult.failed(ex.getMessage() != null ? ex.getMessage() : "Webhook call failed");
        }
    }

    public static final class SyncResult {
        public final boolean success;
        public final boolean skipped;
        public final String error;

        private SyncResult(boolean success, boolean skipped, String error) {
            this.success = success;
            this.skipped = skipped;
            this.error = error;
        }

        public static SyncResult ok() {
            return new SyncResult(true, false, null);
        }

        public static SyncResult skipped(String reason) {
            return new SyncResult(false, true, reason);
        }

        public static SyncResult failed(String error) {
            return new SyncResult(false, false, error);
        }
    }

    private WebhookResponse post(Map<String, Object> body) throws Exception {
        String payload = objectMapper.writeValueAsString(body);

        // Apps Script accepts the POST, then returns 302. The response body is fetched
        // with a GET on the Location URL (POST again returns 405).
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl.get()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        int postStatus = postResponse.statusCode();

        String responseBody;
        int finalStatus;

        if (isRedirect(postStatus)) {
            String location = postResponse.headers().firstValue("location").orElse(null);
            if (location == null || location.isBlank()) {
                throw new IllegalStateException("Webhook redirect missing Location header");
            }

            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(location))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
            finalStatus = getResponse.statusCode();
            responseBody = getResponse.body();
        } else {
            finalStatus = postStatus;
            responseBody = postResponse.body();
        }

        if (finalStatus < 200 || finalStatus > 299) {
            if (responseBody != null && !responseBody.isBlank()) {
                LOG.warnf("Webhook error body: %s", truncate(responseBody, 400));
            }
            throw new IllegalStateException("Webhook returned status " + finalStatus);
        }

        return objectMapper.readValue(responseBody, WebhookResponse.class);
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 307 || status == 308;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }

    private static String slug(String raw) {
        String base = raw == null ? "course" : raw.toLowerCase(Locale.ROOT);
        String normalized = base.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.isBlank()) {
            normalized = "course";
        }
        if (normalized.length() > 52) {
            normalized = normalized.substring(0, 52).replaceAll("-+$", "");
        }
        return normalized + "-veneranda";
    }

    public static class WebhookResponse {
        public Boolean ok;
        public String groupEmail;
        public String error;
    }
}
