package com.ispautomation.modules.course.entity;

import com.ispautomation.common.entity.TenantAwareEntity;
import com.ispautomation.modules.rbac.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_subscriptions")
public class CourseSubscription extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CourseCategory category;

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "PENDING";

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "coordinator_amount", precision = 12, scale = 2)
    private BigDecimal coordinatorAmount;

    @Column(name = "server_fee_amount", precision = 12, scale = 2)
    private BigDecimal serverFeeAmount;

    @Column(nullable = false, length = 3)
    private String currency = "UGX";

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod = "SIMULATED";

    @Column(name = "payment_tx_ref", length = 100)
    private String paymentTxRef;

    @Column(name = "payment_provider_ref", length = 100)
    private String paymentProviderRef;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CourseCategory getCategory() {
        return category;
    }

    public void setCategory(CourseCategory category) {
        this.category = category;
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

    public BigDecimal getCoordinatorAmount() {
        return coordinatorAmount;
    }

    public void setCoordinatorAmount(BigDecimal coordinatorAmount) {
        this.coordinatorAmount = coordinatorAmount;
    }

    public BigDecimal getServerFeeAmount() {
        return serverFeeAmount;
    }

    public void setServerFeeAmount(BigDecimal serverFeeAmount) {
        this.serverFeeAmount = serverFeeAmount;
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

    public String getPaymentTxRef() {
        return paymentTxRef;
    }

    public void setPaymentTxRef(String paymentTxRef) {
        this.paymentTxRef = paymentTxRef;
    }

    public String getPaymentProviderRef() {
        return paymentProviderRef;
    }

    public void setPaymentProviderRef(String paymentProviderRef) {
        this.paymentProviderRef = paymentProviderRef;
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
}
