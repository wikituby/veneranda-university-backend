package com.ispautomation.modules.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.modules.course.dto.CourseSubscriptionDto;
import com.ispautomation.modules.course.entity.CourseSubscription;
import com.ispautomation.modules.course.repository.CourseSubscriptionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApplicationScoped
public class FlutterwavePaymentService {

    private static final Logger LOG = Logger.getLogger(FlutterwavePaymentService.class);

    @Inject
    FlutterwaveClient flutterwaveClient;

    @Inject
    PaymentSettingsService paymentSettings;

    @Inject
    CourseSubscriptionRepository subscriptionRepository;

    public boolean validateWebhookSignature(String verifHash) {
        String expected = paymentSettings.webhookHash();
        if (expected == null || expected.isBlank()) {
            LOG.warn("Flutterwave webhook hash is not configured; rejecting webhook");
            return false;
        }
        return expected.equals(verifHash);
    }

    @Transactional
    public CourseSubscriptionDto verifyAndComplete(String txRef, Long userId) {
        if (txRef == null || txRef.isBlank()) {
            throw new BusinessException(400, "Missing payment reference.");
        }
        CourseSubscription subscription = subscriptionRepository.findByPaymentTxRef(txRef.trim())
                .orElseThrow(() -> new BusinessException(404, "No subscription found for this payment."));

        if (userId != null && subscription.getUser() != null
                && !userId.equals(subscription.getUser().getId())) {
            throw new BusinessException(403, "This payment does not belong to your account.");
        }

        if (isPaidActive(subscription)) {
            return CourseSubscriptionDto.fromEntity(subscription);
        }

        JsonNode data = flutterwaveClient.verifyByTxRef(txRef.trim());
        return applyVerifiedCharge(subscription, data);
    }

    @Transactional
    public CourseSubscriptionDto verifyAndComplete(String txRef) {
        return verifyAndComplete(txRef, null);
    }

    @Transactional
    public CourseSubscriptionDto handleWebhook(JsonNode payload) {
        JsonNode data = payload != null && payload.has("data") ? payload.get("data") : payload;
        if (data == null || data.isMissingNode()) {
            throw new BusinessException(400, "Invalid Flutterwave webhook payload.");
        }
        String txRef = text(data, "tx_ref");
        if (txRef == null) {
            throw new BusinessException(400, "Webhook missing tx_ref.");
        }
        CourseSubscription subscription = subscriptionRepository.findByPaymentTxRef(txRef)
                .orElseThrow(() -> new BusinessException(404, "No subscription found for tx_ref."));

        if (isPaidActive(subscription)) {
            return CourseSubscriptionDto.fromEntity(subscription);
        }

        // Always re-verify with Flutterwave before unlocking content
        JsonNode verified = flutterwaveClient.verifyByTxRef(txRef);
        return applyVerifiedCharge(subscription, verified);
    }

    private CourseSubscriptionDto applyVerifiedCharge(CourseSubscription subscription, JsonNode data) {
        String status = text(data, "status");
        if (!"successful".equalsIgnoreCase(status)) {
            subscription.setPaymentStatus("FAILED");
            subscriptionRepository.persist(subscription);
            throw new BusinessException(400, "Payment was not successful (status: " + status + ").");
        }

        String currency = text(data, "currency");
        String expectedCurrency = paymentSettings.paymentCurrency();
        if (currency != null && !currency.equalsIgnoreCase(expectedCurrency)) {
            LOG.warnf("Currency mismatch for %s: expected %s got %s", subscription.getPaymentTxRef(), expectedCurrency, currency);
            throw new BusinessException(400, "Payment currency mismatch.");
        }

        BigDecimal paidAmount = decimal(data, "amount");
        if (paidAmount != null && subscription.getAmount() != null
                && paidAmount.compareTo(subscription.getAmount()) < 0) {
            LOG.warnf("Amount mismatch for %s: expected %s got %s",
                    subscription.getPaymentTxRef(), subscription.getAmount(), paidAmount);
            throw new BusinessException(400, "Payment amount is less than the subscription total.");
        }

        String flwId = text(data, "id");
        if (flwId == null) {
            flwId = text(data, "flw_ref");
        }

        subscription.setPaymentStatus("PAID");
        subscription.setPaymentMethod("FLUTTERWAVE");
        subscription.setPaymentProviderRef(flwId);
        subscription.setPaidAt(LocalDateTime.now());
        subscription.setExpiresAt(null);
        subscription.setStatus("ACTIVE");
        subscriptionRepository.persist(subscription);
        LOG.infof("Subscription %s marked PAID via Flutterwave tx_ref=%s", subscription.getUuid(), subscription.getPaymentTxRef());
        return CourseSubscriptionDto.fromEntity(subscription);
    }

    private static boolean isPaidActive(CourseSubscription subscription) {
        return "PAID".equals(subscription.getPaymentStatus())
                && "ACTIVE".equals(subscription.getStatus())
                && (subscription.getExpiresAt() == null || subscription.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
