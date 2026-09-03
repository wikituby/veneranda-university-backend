package com.ispautomation.modules.payment.dto;

import com.ispautomation.modules.course.dto.CourseSubscriptionDto;

public class CheckoutResponseDto {

    private CourseSubscriptionDto subscription;
    private boolean requiresRedirect;
    private String paymentLink;
    private String txRef;
    private String currency;

    public static CheckoutResponseDto paid(CourseSubscriptionDto subscription) {
        CheckoutResponseDto dto = new CheckoutResponseDto();
        dto.subscription = subscription;
        dto.requiresRedirect = false;
        dto.currency = subscription != null ? subscription.getCurrency() : null;
        return dto;
    }

    public static CheckoutResponseDto redirect(
            CourseSubscriptionDto subscription,
            String paymentLink,
            String txRef,
            String currency
    ) {
        CheckoutResponseDto dto = new CheckoutResponseDto();
        dto.subscription = subscription;
        dto.requiresRedirect = true;
        dto.paymentLink = paymentLink;
        dto.txRef = txRef;
        dto.currency = currency;
        return dto;
    }

    public CourseSubscriptionDto getSubscription() {
        return subscription;
    }

    public void setSubscription(CourseSubscriptionDto subscription) {
        this.subscription = subscription;
    }

    public boolean isRequiresRedirect() {
        return requiresRedirect;
    }

    public void setRequiresRedirect(boolean requiresRedirect) {
        this.requiresRedirect = requiresRedirect;
    }

    public String getPaymentLink() {
        return paymentLink;
    }

    public void setPaymentLink(String paymentLink) {
        this.paymentLink = paymentLink;
    }

    public String getTxRef() {
        return txRef;
    }

    public void setTxRef(String txRef) {
        this.txRef = txRef;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
