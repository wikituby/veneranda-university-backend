package com.ispautomation.modules.rbac.repository;

import com.ispautomation.modules.rbac.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Repository for {@link User} entities.
 */
@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, Long> {

    /**
     * Find a user by username within a tenant.
     */
    public Optional<User> findByTenantAndUsername(Long tenantId, String username) {
        return find("tenant.id = ?1 and username = ?2", tenantId, username).firstResultOptional();
    }

    /**
     * Find a user by email (globally unique).
     */
    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public Optional<User> findByGoogleSub(String googleSub) {
        return find("googleSub", googleSub).firstResultOptional();
    }

    /**
     * Check if a username already exists within a tenant.
     */
    public boolean existsByUsername(Long tenantId, String username) {
        return count("tenant.id = ?1 and username = ?2", tenantId, username) > 0;
    }

    /**
     * Check if an email already exists.
     */
    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
}