package com.ispautomation.modules.course.repository;

import com.ispautomation.modules.course.entity.CourseSubscription;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CourseSubscriptionRepository implements PanacheRepositoryBase<CourseSubscription, Long> {

    public Optional<CourseSubscription> findByUserAndCategory(Long userId, Long categoryId) {
        return find("user.id = ?1 and category.id = ?2", userId, categoryId).firstResultOptional();
    }

    public Optional<CourseSubscription> findByPaymentTxRef(String txRef) {
        if (txRef == null || txRef.isBlank()) {
            return Optional.empty();
        }
        return find("paymentTxRef = ?1", txRef.trim()).firstResultOptional();
    }

    public List<CourseSubscription> findPaidByUser(Long tenantId, Long userId) {
        return list(
                "tenant.id = ?1 and user.id = ?2 and paymentStatus = ?3 and status = ?4 "
                        + "and (expiresAt is null or expiresAt > ?5)",
                tenantId,
                userId,
                "PAID",
                "ACTIVE",
                java.time.LocalDateTime.now()
        );
    }

    public List<CourseSubscription> findByUser(Long tenantId, Long userId) {
        return list("tenant.id = ?1 and user.id = ?2 and status = ?3", tenantId, userId, "ACTIVE");
    }

    public long countDistinctPaidUsers(Long tenantId, java.util.Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return 0L;
        }
        Number count = getEntityManager()
                .createQuery(
                        "select count(distinct s.user.id) from CourseSubscription s "
                                + "where s.tenant.id = :tenantId and s.category.id in :ids "
                                + "and s.paymentStatus = 'PAID' and s.status = 'ACTIVE'",
                        Long.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("ids", categoryIds)
                .getSingleResult();
        return count != null ? count.longValue() : 0L;
    }

    public List<CourseSubscription> findPaidSince(
            Long tenantId,
            java.util.Collection<Long> categoryIds,
            java.time.LocalDateTime since
    ) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        return getEntityManager()
                .createQuery(
                        "select s from CourseSubscription s where s.tenant.id = :tenantId "
                                + "and s.category.id in :ids and s.paymentStatus = 'PAID' "
                                + "and s.status = 'ACTIVE' and s.paidAt is not null and s.paidAt >= :since",
                        CourseSubscription.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("ids", categoryIds)
                .setParameter("since", since)
                .getResultList();
    }

    public java.math.BigDecimal sumCoordinatorRevenue(Long tenantId, java.util.Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        Object sum = getEntityManager()
                .createQuery(
                        "select coalesce(sum(s.coordinatorAmount), 0) from CourseSubscription s "
                                + "where s.tenant.id = :tenantId and s.category.id in :ids "
                                + "and s.paymentStatus = 'PAID' and s.status = 'ACTIVE'"
                )
                .setParameter("tenantId", tenantId)
                .setParameter("ids", categoryIds)
                .getSingleResult();
        if (sum instanceof java.math.BigDecimal) {
            return (java.math.BigDecimal) sum;
        }
        if (sum instanceof Number) {
            return java.math.BigDecimal.valueOf(((Number) sum).doubleValue());
        }
        return java.math.BigDecimal.ZERO;
    }
}
