package com.ispautomation.modules.rbac.repository;

import com.ispautomation.modules.rbac.entity.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Repository for {@link RefreshToken} entities.
 */
@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshToken, Long> {

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }

    public long revokeAllForUser(Long userId) {
        return update("isRevoked = true, revokedAt = now() where user.id = ?1 and isRevoked = false", userId);
    }
}