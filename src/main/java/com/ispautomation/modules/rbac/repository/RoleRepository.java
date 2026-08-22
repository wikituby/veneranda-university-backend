package com.ispautomation.modules.rbac.repository;

import com.ispautomation.modules.rbac.entity.Role;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Repository for {@link Role} entities.
 */
@ApplicationScoped
public class RoleRepository implements PanacheRepositoryBase<Role, Long> {

    public Optional<Role> findByTenantAndCode(Long tenantId, String code) {
        return find("tenant.id = ?1 and code = ?2", tenantId, code).firstResultOptional();
    }

    public boolean existsByTenantAndCode(Long tenantId, String code) {
        return count("tenant.id = ?1 and code = ?2", tenantId, code) > 0;
    }
}