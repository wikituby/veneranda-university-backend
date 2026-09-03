package com.ispautomation.modules.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ispautomation.common.exception.BusinessException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class FlutterwaveClient {

    private static final Logger LOG = Logger.getLogger(FlutterwaveClient.class);
    private static final String BASE = "https://api.flutterwave.com/v3";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    PaymentSettingsService paymentSettings;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public String initializePayment(
            String txRef,
            BigDecimal amount,
            String currency,
            String redirectUrl,
            String paymentOptions,
            String customerEmail,
            String customerName,
            String customerPhone,
            String title,
            String description,
            Map<String, String> meta
    ) {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("email", customerEmail != null && !customerEmail.isBlank()
                ? customerEmail
                : "student@" + txRef + ".local");
        if (customerName != null && !customerName.isBlank()) {
            customer.put("name", customerName);
        }
        if (customerPhone != null && !customerPhone.isBlank()) {
            customer.put("phonenumber", customerPhone.replaceAll("\\s+", ""));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tx_ref", txRef);
        body.put("amount", amount.stripTrailingZeros().toPlainString());
        body.put("currency", currency);
        body.put("redirect_url", redirectUrl);
        body.put("payment_options", paymentOptions);
        body.put("customer", customer);
        body.put("customizations", Map.of(
                "title", title != null ? title : "Subscribe",
                "description", description != null ? description : "Programme subscription"
        ));
        if (meta != null && !meta.isEmpty()) {
            body.put("meta", meta);
        }

        JsonNode data = post("/payments", body);
        JsonNode link = data.path("link");
        if (link.isMissingNode() || link.asText().isBlank()) {
            throw new BusinessException(502, "Flutterwave did not return a payment link.");
        }
        return link.asText();
    }

    public JsonNode verifyByTxRef(String txRef) {
        return get("/transactions/verify_by_reference?tx_ref=" + encode(txRef));
    }

    public JsonNode verifyById(long transactionId) {
        return get("/transactions/" + transactionId + "/verify");
    }

    private JsonNode post(String path, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + requireSecret())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return parseResponse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Flutterwave POST failed", ex);
            throw new BusinessException(502, "Could not reach Flutterwave. Try again shortly.");
        }
    }

    private JsonNode get(String pathAndQuery) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + pathAndQuery))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + requireSecret())
                    .GET()
                    .build();
            return parseResponse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Flutterwave GET failed", ex);
            throw new BusinessException(502, "Could not verify payment with Flutterwave.");
        }
    }

    private JsonNode parseResponse(HttpResponse<String> response) throws Exception {
        JsonNode root = objectMapper.readTree(response.body() != null ? response.body() : "{}");
        String status = root.path("status").asText("");
        if (response.statusCode() >= 400 || !"success".equalsIgnoreCase(status)) {
            String message = root.path("message").asText("Flutterwave request failed");
            LOG.warnf("Flutterwave error status=%s body=%s", response.statusCode(), response.body());
            throw new BusinessException(502, message);
        }
        return root.path("data");
    }

    private String requireSecret() {
        String key = paymentSettings.secretKey();
        if (key == null || key.isBlank()) {
            throw new BusinessException(500, "Flutterwave secret key is not configured in settings.");
        }
        return key;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
