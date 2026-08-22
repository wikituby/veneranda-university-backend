package com.ispautomation.modules.rbac.repository;

import com.ispautomation.modules.rbac.entity.Permission;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Permission} entities.
 */
@ApplicationScoped
public class PermissionRepository implements PanacheRepositoryBase<Permission, Long> {

    public Optional<Permission> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }

    public List<Permission> findByModule(String module) {
        return list("module", module);
    }

    public boolean existsByCode(String code) {
        return count("code", code) > 0;
    }
}