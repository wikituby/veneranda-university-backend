package com.ispautomation.modules.audit.repository;

import com.ispautomation.modules.audit.entity.AuditLog;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repository for {@link AuditLog} entities (append-only).
 */
@ApplicationScoped
public class AuditLogRepository implements PanacheRepositoryBase<AuditLog, Long> {
}