package com.ispautomation.modules.course.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;

@ApplicationScoped
public class LmsPricingConfig {

    @ConfigProperty(name = "lms.server-fee-amount", defaultValue = "5000")
    BigDecimal serverFeeAmount;

    @ConfigProperty(name = "lms.default-currency", defaultValue = "UGX")
    String defaultCurrency;

    public BigDecimal serverFeeAmount() {
        return serverFeeAmount != null ? serverFeeAmount : new BigDecimal("5000");
    }

    public String defaultCurrency() {
        return defaultCurrency != null && !defaultCurrency.isBlank()
                ? defaultCurrency.trim().toUpperCase()
                : "UGX";
    }
}
