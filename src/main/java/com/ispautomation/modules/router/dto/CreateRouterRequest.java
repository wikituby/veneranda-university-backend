package com.ispautomation.modules.router.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new router.
 */
public class CreateRouterRequest {

    @NotBlank(message = "Router name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Vendor is required")
    @Size(max = 50, message = "Vendor must not exceed 50 characters")
    private String vendor;

    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    @NotBlank(message = "IP address is required")
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;

    @NotNull(message = "API port is required")
    @Min(value = 1, message = "API port must be at least 1")
    @Max(value = 65535, message = "API port must not exceed 65535")
    private Integer apiPort = 8728;

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must not exceed 100 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password must not exceed 255 characters")
    private String password;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 100, message = "Firmware must not exceed 100 characters")
    private String firmware;

    @Size(max = 50, message = "Router version must not exceed 50 characters")
    private String routerVersion;

    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serialNumber;

    private Long branchId;

    private Boolean isEnabled = true;

    private String notes;

    // ===== Getters / Setters =====

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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }

    public String getRouterVersion() { return routerVersion; }
    public void setRouterVersion(String routerVersion) { this.routerVersion = routerVersion; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}