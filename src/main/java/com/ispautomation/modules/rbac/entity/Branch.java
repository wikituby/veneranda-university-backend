package com.ispautomation.modules.rbac.entity;


import com.ispautomation.common.entity.TenantAwareEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;


@Entity
@Table(name = "branches")
public class Branch extends TenantAwareEntity {


    @Column(name = "code", nullable = false, length = 50)
    private String code;


    @Column(name = "name", nullable = false, length = 150)
    private String name;


    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    @Column(name = "contact_email", length = 150)
    private String contactEmail;


    @Column(name = "contact_phone", length = 50)
    private String contactPhone;


    @Column(name = "address", columnDefinition = "TEXT")
    private String address;


    @Column(name = "district", length = 100)
    private String district;


    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;


    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;


    @Column(name = "manager_id")
    private Long managerId;


    @Column(name = "is_head_office", nullable = false)
    private Boolean isHeadOffice = false;


    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;



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


    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }


    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }


    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }


    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }


    public Boolean getIsHeadOffice() {
        return isHeadOffice;
    }

    public void setIsHeadOffice(Boolean isHeadOffice) {
        this.isHeadOffice = isHeadOffice;
    }


    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

}