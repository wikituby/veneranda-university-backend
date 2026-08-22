package com.ispautomation.modules.rbac.entity;


import com.ispautomation.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {


    @Column(name = "name", nullable = false, length = 100)
    private String name;


    @Column(name = "code", nullable = false, length = 100, unique = true)
    private String code;


    @Column(name = "module", nullable = false, length = 50)
    private String module;


    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = true;



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



    public String getModule() {
        return module;
    }


    public void setModule(String module) {
        this.module = module;
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

}