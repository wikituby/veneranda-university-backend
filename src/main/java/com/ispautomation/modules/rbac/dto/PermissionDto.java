package com.ispautomation.modules.rbac.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Permission DTO for API responses.
 */
public class PermissionDto {

    private Long id;
    private UUID uuid;
    private String name;
    private String code;
    private String module;
    private String description;
    private Boolean isSystem;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PermissionDto() {
    }

    public static PermissionDto fromEntity(com.ispautomation.modules.rbac.entity.Permission permission) {
        PermissionDto dto = new PermissionDto();
        dto.id = permission.getId();
        dto.uuid = permission.getUuid();
        dto.name = permission.getName();
        dto.code = permission.getCode();
        dto.module = permission.getModule();
        dto.description = permission.getDescription();
        dto.isSystem = permission.getIsSystem();
        dto.status = permission.getStatus();
        dto.createdAt = permission.getCreatedAt();
        dto.updatedAt = permission.getUpdatedAt();
        return dto;
    }

    // ===== Getters / Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}