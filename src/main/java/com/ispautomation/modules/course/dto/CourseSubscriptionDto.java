package com.ispautomation.modules.course.dto;

import com.ispautomation.modules.course.entity.CourseSubscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CourseSubscriptionDto {

    private String id;
    private String categoryId;
    private String categoryTitle;
    private String nodeKind;
    private String paymentStatus;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    private boolean paid;

    public static CourseSubscriptionDto fromEntity(CourseSubscription entity) {
        CourseSubscriptionDto dto = new CourseSubscriptionDto();
        dto.id = entity.getUuid() != null ? entity.getUuid().toString() : null;
        dto.categoryId = entity.getCategory() != null && entity.getCategory().getUuid() != null
                ? entity.getCategory().getUuid().toString()
                : null;
        dto.categoryTitle = entity.getCategory() != null ? entity.getCategory().getTitle() : null;
        dto.nodeKind = entity.getCategory() != null ? entity.getCategory().getNodeKind() : null;
        dto.paymentStatus = entity.getPaymentStatus();
        dto.amount = entity.getAmount();
        dto.currency = entity.getCurrency();
        dto.paymentMethod = entity.getPaymentMethod();
        dto.paidAt = entity.getPaidAt();
        dto.expiresAt = entity.getExpiresAt();
        dto.paid = "PAID".equals(entity.getPaymentStatus())
                && (entity.getExpiresAt() == null || entity.getExpiresAt().isAfter(java.time.LocalDateTime.now()));
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryTitle() {
        return categoryTitle;
    }

    public void setCategoryTitle(String categoryTitle) {
        this.categoryTitle = categoryTitle;
    }

    public String getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
}
