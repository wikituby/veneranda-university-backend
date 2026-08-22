package com.ispautomation.modules.settings.repository;

import com.ispautomation.modules.settings.entity.Setting;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Setting} entities.
 */
@ApplicationScoped
public class SettingRepository implements PanacheRepositoryBase<Setting, Long> {

    public Optional<Setting> findByCategoryAndKey(Long tenantId, String category, String key) {
        if (tenantId == null) {
            return find("tenantId is null and category = ?1 and key = ?2", category, key).firstResultOptional();
        }
        return find("tenantId = ?1 and category = ?2 and key = ?3", tenantId, category, key).firstResultOptional();
    }

    public List<Setting> findByTenantId(Long tenantId) {
        if (tenantId == null) {
            return list("tenantId is null");
        }
        return list("tenantId = ?1 or tenantId is null", tenantId);
    }

    public List<Setting> findPublicSettings() {
        return list("isPublic = true and status = 'ACTIVE'");
    }

    public List<Setting> findByCategory(String category) {
        return list("category = ?1 and status = 'ACTIVE'", category);
    }
}