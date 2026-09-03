package com.ispautomation.modules.payment.service;

import com.ispautomation.modules.settings.entity.Setting;
import com.ispautomation.modules.settings.repository.SettingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * Reads payment-related settings (DB) with env/config fallbacks.
 * Flutterwave charges use {@link #paymentCurrency()} — default UGX.
 */
@ApplicationScoped
public class PaymentSettingsService {

    private static final String CATEGORY = "payment";

    @Inject
    SettingRepository settingRepository;

    @ConfigProperty(name = "lms.default-currency", defaultValue = "UGX")
    String defaultCurrency;

    @ConfigProperty(name = "flutterwave.enabled", defaultValue = "true")
    boolean envFlutterwaveEnabled;

    @ConfigProperty(name = "flutterwave.secret-key")
    Optional<String> envSecretKey;

    @ConfigProperty(name = "flutterwave.public-key")
    Optional<String> envPublicKey;

    @ConfigProperty(name = "flutterwave.webhook-hash")
    Optional<String> envWebhookHash;

    @ConfigProperty(name = "flutterwave.frontend-base-url", defaultValue = "https://educ.venerandahospital.org")
    String envFrontendBaseUrl;

    public boolean isFlutterwaveEnabled() {
        // Env wins when explicitly set (Render / local .env)
        if (envFlutterwaveEnabled) {
            return true;
        }
        String fromDb = settingValue("flutterwave_enabled");
        if (fromDb != null && !fromDb.isBlank()) {
            return "true".equalsIgnoreCase(fromDb.trim());
        }
        return false;
    }

    public String paymentCurrency() {
        String fromDb = settingValue("payment_currency");
        if (fromDb != null && !fromDb.isBlank()) {
            return fromDb.trim().toUpperCase();
        }
        return defaultCurrency != null && !defaultCurrency.isBlank()
                ? defaultCurrency.trim().toUpperCase()
                : "UGX";
    }

    public String secretKey() {
        // Prefer env secrets in production so keys are not stored in plain settings rows
        String fromEnv = envSecretKey.filter(s -> !s.isBlank()).orElse("");
        if (!fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromDb = settingValue("flutterwave_secret_key");
        return fromDb != null ? fromDb.trim() : "";
    }

    public String publicKey() {
        String fromEnv = envPublicKey.filter(s -> !s.isBlank()).orElse("");
        if (!fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromDb = settingValue("flutterwave_public_key");
        return fromDb != null ? fromDb.trim() : "";
    }

    public String webhookHash() {
        String fromEnv = envWebhookHash.filter(s -> !s.isBlank()).orElse("");
        if (!fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromDb = settingValue("flutterwave_webhook_hash");
        return fromDb != null ? fromDb.trim() : "";
    }

    public String frontendBaseUrl() {
        String fromDb = settingValue("frontend_base_url");
        if (fromDb != null && !fromDb.isBlank() && !fromDb.contains("localhost")) {
            return stripTrailingSlash(fromDb.trim());
        }
        return stripTrailingSlash(envFrontendBaseUrl != null && !envFrontendBaseUrl.isBlank()
                ? envFrontendBaseUrl
                : "https://educ.venerandahospital.org");
    }

    public boolean isConfigured() {
        return !secretKey().isBlank();
    }

    private String settingValue(String key) {
        Optional<Setting> setting = settingRepository.findByCategoryAndKey(null, CATEGORY, key);
        return setting.map(Setting::getValue).orElse(null);
    }

    private static String stripTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
