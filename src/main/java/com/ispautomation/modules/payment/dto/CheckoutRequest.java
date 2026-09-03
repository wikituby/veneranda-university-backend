package com.ispautomation.modules.payment.dto;

public class CheckoutRequest {

    /** visa | mtn | airtel — preferred channel on Flutterwave hosted checkout */
    private String paymentMethod;
    private String phone;

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
