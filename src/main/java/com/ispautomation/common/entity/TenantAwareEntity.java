package com.ispautomation.common.entity;


import com.ispautomation.modules.rbac.entity.Tenant;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;



@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "tenant_id",
        nullable = false
    )
    private Tenant tenant;



    public Tenant getTenant() {
        return tenant;
    }


    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}