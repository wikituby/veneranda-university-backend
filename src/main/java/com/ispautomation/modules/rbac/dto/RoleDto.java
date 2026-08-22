package com.ispautomation.modules.rbac.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Role DTO for API responses.
 */
public class RoleDto {

    private Long id;
    private UUID uuid;
    private String name;
    private String code;
    private String description;
    private Long tenantId;
    private Boolean isSystem;
    private Boolean isActive;
    private Set<PermissionDto> permissions;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RoleDto() {
    }

    public static RoleDto fromEntity(com.ispautomation.modules.rbac.entity.Role role) {
        RoleDto dto = new RoleDto();
        dto.id = role.getId();
        dto.uuid = role.getUuid();
        dto.name = role.getName();
        dto.code = role.getCode();
        dto.description = role.getDescription();
        dto.tenantId = role.getTenant() != null ? role.getTenant().getId() : null;
        dto.isSystem = role.getIsSystem();
        dto.isActive = role.getIsActive();
        dto.permissions = role.getPermissions().stream()
                .map(PermissionDto::fromEntity)
                .collect(Collectors.toSet());
        dto.status = role.getStatus();
        dto.createdAt = role.getCreatedAt();
        dto.updatedAt = role.getUpdatedAt();
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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Set<PermissionDto> getPermissions() { return permissions; }
    public void setPermissions(Set<PermissionDto> permissions) { this.permissions = permissions; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}