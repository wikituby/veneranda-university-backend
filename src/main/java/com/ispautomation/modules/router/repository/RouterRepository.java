package com.ispautomation.modules.router.repository;

import com.ispautomation.modules.router.entity.Router;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Repository for {@link Router} entities.
 */
@ApplicationScoped
public class RouterRepository implements PanacheRepositoryBase<Router, Long> {

    /**
     * Find a router by IP address within a tenant.
     */
    public Optional<Router> findByTenantAndIpAddress(Long tenantId, String ipAddress) {
        return find("tenant.id = ?1 and ipAddress = ?2", tenantId, ipAddress).firstResultOptional();
    }

    /**
     * Check if an IP address already exists within a tenant.
     */
    public boolean existsByTenantAndIpAddress(Long tenantId, String ipAddress) {
        return count("tenant.id = ?1 and ipAddress = ?2", tenantId, ipAddress) > 0;
    }

    /**
     * Count routers by tenant.
     */
    public long countByTenant(Long tenantId) {
        return count("tenant.id = ?1", tenantId);
    }

    /**
     * Count online routers by tenant.
     */
    public long countOnlineByTenant(Long tenantId) {
        return count("tenant.id = ?1 and isOnline = true", tenantId);
    }

    /**
     * Count routers by vendor within a tenant.
     */
    public long countByTenantAndVendor(Long tenantId, Router.Vendor vendor) {
        return count("tenant.id = ?1 and vendor = ?2", tenantId, vendor);
    }
}