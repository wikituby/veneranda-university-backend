package com.ispautomation.modules.router.dto;

import com.ispautomation.modules.router.entity.Router;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Router DTO for API responses (never exposes decrypted password).
 */
public class RouterDto {

    private Long id;
    private UUID uuid;
    private String name;
    private String vendor;
    private String model;
    private String ipAddress;
    private Integer apiPort;
    private String username;
    private String location;
    private String firmware;
    private String routerVersion;
    private String serialNumber;
    private Boolean isEnabled;
    private Boolean isOnline;
    private LocalDateTime lastSyncAt;
    private LocalDateTime lastSeenAt;
    private String notes;
    private Long tenantId;
    private String tenantName;
    private Long branchId;
    private String branchName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RouterDto() {
    }

    public static RouterDto fromEntity(Router router) {
        RouterDto dto = new RouterDto();
        dto.id = router.getId();
        dto.uuid = router.getUuid();
        dto.name = router.getName();
        dto.vendor = router.getVendor() != null ? router.getVendor().name() : null;
        dto.model = router.getModel();
        dto.ipAddress = router.getIpAddress();
        dto.apiPort = router.getApiPort();
        dto.username = router.getUsername();
        dto.location = router.getLocation();
        dto.firmware = router.getFirmware();
        dto.routerVersion = router.getRouterVersion();
        dto.serialNumber = router.getSerialNumber();
        dto.isEnabled = router.getIsEnabled();
        dto.isOnline = router.getIsOnline();
        dto.lastSyncAt = router.getLastSyncAt();
        dto.lastSeenAt = router.getLastSeenAt();
        dto.notes = router.getNotes();
        dto.tenantId = router.getTenant() != null ? router.getTenant().getId() : null;
        dto.tenantName = router.getTenant() != null ? router.getTenant().getName() : null;
        dto.branchId = router.getBranch() != null ? router.getBranch().getId() : null;
        dto.branchName = router.getBranch() != null ? router.getBranch().getName() : null;
        dto.status = router.getStatus();
        dto.createdAt = router.getCreatedAt();
        dto.updatedAt = router.getUpdatedAt();
        return dto;
    }

    // ===== Getters / Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Integer getApiPort() { return apiPort; }
    public void setApiPort(Integer apiPort) { this.apiPort = apiPort; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}