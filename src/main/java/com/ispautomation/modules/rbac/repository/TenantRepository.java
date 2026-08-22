package com.ispautomation.modules.rbac.repository;

import com.ispautomation.modules.rbac.entity.Tenant;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class TenantRepository implements PanacheRepositoryBase<Tenant, Long> {

    public Optional<Tenant> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }
}
