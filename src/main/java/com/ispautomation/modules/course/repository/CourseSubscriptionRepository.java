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
}
