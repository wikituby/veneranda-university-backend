package com.ispautomation.modules.router.entity;

import com.ispautomation.common.entity.TenantAwareEntity;
import com.ispautomation.modules.rbac.entity.Branch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Represents a managed network router/device.
 *
 * Supports multiple vendors: MikroTik, Ubiquiti, TP-Link, D-Link,
 * Cisco, Huawei, and generic RouterOS devices.
 */
@Entity
@Table(name = "routers")
public class Router extends TenantAwareEntity {

    public enum Vendor {
        MIKROTIK,
        UBIQUITI,
        TP_LINK,
        D_LINK,
        CISCO,
        HUAWEI,
        GENERIC
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Vendor vendor;

    @Column(length = 100)
    private String model;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "api_port", nullable = false)
    private Integer apiPort = 8728;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "password_encrypted", nullable = false, columnDefinition = "TEXT")
    private String passwordEncrypted;

    @Column(length = 255)
    private String location;

    @Column(length = 100)
    private String firmware;

    @Column(name = "router_version", length = 50)
    private String routerVersion;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ===== Getters / Setters =====

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Integer getApiPort() { return apiPort; }
    public void setApiPort(Integer apiPort) { this.apiPort = apiPort; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordEncrypted() { return passwordEncrypted; }
    public void setPasswordEncrypted(String passwordEncrypted) { this.passwordEncrypted = passwordEncrypted; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }

    public String getRouterVersion() { return routerVersion; }
    public void setRouterVersion(String routerVersion) { this.routerVersion = routerVersion; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}