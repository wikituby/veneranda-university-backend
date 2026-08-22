package com.ispautomation.modules.rbac.entity;


import com.ispautomation.common.entity.TenantAwareEntity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;



@Entity
@Table(name = "roles")
public class Role extends TenantAwareEntity {


    @Column(name = "name", nullable = false, length = 50)
    private String name;



    @Column(name = "code", nullable = false, length = 50)
    private String code;



    @Column(name = "description", columnDefinition = "TEXT")
    private String description;



    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;



    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;



    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();



    // ================= GETTERS / SETTERS =================


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }



    public String getCode() {
        return code;
    }


    public void setCode(String code) {
        this.code = code;
    }



    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }



    public Boolean getIsSystem() {
        return isSystem;
    }


    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }



    public Boolean getIsActive() {
        return isActive;
    }


    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }



    public Set<Permission> getPermissions() {
        return permissions;
    }


    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

}