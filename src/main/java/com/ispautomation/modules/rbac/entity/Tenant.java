package com.ispautomation.modules.rbac.entity;


import com.ispautomation.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;



@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {



    @Column(
            name = "code",
            nullable = false,
            length = 50
    )
    private String code;



    @Column(
            name = "name",
            nullable = false,
            length = 150
    )
    private String name;



    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;



    @Column(
            name = "contact_email",
            length = 150
    )
    private String contactEmail;



    @Column(
            name = "contact_phone",
            length = 50
    )
    private String contactPhone;



    @Column(
            name = "address",
            columnDefinition = "TEXT"
    )
    private String address;



    @Column(
            name = "logo_url",
            length = 500
    )
    private String logoUrl;



    @Column(
            name = "primary_color",
            length = 20
    )
    private String primaryColor = "#1976d2";



    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;



    @Column(
            name = "plan",
            nullable = false,
            length = 30
    )
    private String plan = "TRIAL";



    @Column(
            name = "trial_ends_at"
    )
    private LocalDateTime trialEndsAt;



    // ================= GETTERS / SETTERS =================


    public String getCode() {
        return code;
    }


    public void setCode(String code) {
        this.code = code;
    }



    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }



    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }



    public String getContactEmail() {
        return contactEmail;
    }


    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }



    public String getContactPhone() {
        return contactPhone;
    }


    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }



    public String getAddress() {
        return address;
    }


    public void setAddress(String address) {
        this.address = address;
    }



    public String getLogoUrl() {
        return logoUrl;
    }


    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }



    public String getPrimaryColor() {
        return primaryColor;
    }


    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }



    public Boolean getIsActive() {
        return isActive;
    }


    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }



    public String getPlan() {
        return plan;
    }


    public void setPlan(String plan) {
        this.plan = plan;
    }



    public LocalDateTime getTrialEndsAt() {
        return trialEndsAt;
    }


    public void setTrialEndsAt(LocalDateTime trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

}