package com.ispautomation.modules.course.repository;

import com.ispautomation.modules.course.entity.CourseEnrollment;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CourseEnrollmentRepository implements PanacheRepositoryBase<CourseEnrollment, Long> {

    public Optional<CourseEnrollment> findByUserAndCategory(Long userId, Long categoryId) {
        return find("user.id = ?1 and category.id = ?2", userId, categoryId).firstResultOptional();
    }

    public List<CourseEnrollment> findActiveByUser(Long tenantId, Long userId) {
        return list(
                "tenant.id = ?1 and user.id = ?2 and enrollmentStatus = ?3 and status = ?4",
                tenantId,
                userId,
                "ACTIVE",
                "ACTIVE"
        );
    }

    public List<CourseEnrollment> findPendingByCategory(Long tenantId, Long categoryId) {
        return list(
                "tenant.id = ?1 and category.id = ?2 and enrollmentStatus = ?3 and status = ?4",
                tenantId,
                categoryId,
                "PENDING",
                "ACTIVE"
        );
    }

    public long countByCategoryAndStatus(Long tenantId, Long categoryId, String enrollmentStatus) {
        return count(
                "tenant.id = ?1 and category.id = ?2 and enrollmentStatus = ?3 and status = ?4",
                tenantId,
                categoryId,
                enrollmentStatus,
                "ACTIVE"
        );
    }

    public List<CourseEnrollment> findPendingByCreator(Long tenantId, Long creatorId) {
        return getEntityManager()
                .createQuery(
                        "select e from CourseEnrollment e join fetch e.user join fetch e.category c "
                                + "where e.tenant.id = :tenantId and c.createdBy = :creatorId "
                                + "and e.enrollmentStatus = 'PENDING' and e.status = 'ACTIVE' "
                                + "order by e.enrolledAt desc",
                        CourseEnrollment.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("creatorId", creatorId)
                .getResultList();
    }

    public List<CourseEnrollment> findActiveEnrollmentsForRootsSince(
            Long tenantId,
            List<Long> rootCategoryIds,
            java.time.LocalDateTime since
    ) {
        if (rootCategoryIds == null || rootCategoryIds.isEmpty()) {
            return List.of();
        }
        return getEntityManager()
                .createQuery(
                        "select e from CourseEnrollment e where e.tenant.id = :tenantId "
                                + "and e.category.id in :ids and e.enrollmentStatus = 'ACTIVE' "
                                + "and e.status = 'ACTIVE' and e.enrolledAt is not null "
                                + "and e.enrolledAt >= :since",
                        CourseEnrollment.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("ids", rootCategoryIds)
                .setParameter("since", since)
                .getResultList();
    }

    public List<CourseEnrollment> findRecentActiveByCreator(Long tenantId, Long creatorId, int limit) {
        return getEntityManager()
                .createQuery(
                        "select e from CourseEnrollment e join fetch e.user join fetch e.category c "
                                + "where e.tenant.id = :tenantId and c.createdBy = :creatorId "
                                + "and e.enrollmentStatus = 'ACTIVE' and e.status = 'ACTIVE' "
                                + "order by e.enrolledAt desc",
                        CourseEnrollment.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("creatorId", creatorId)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<CourseEnrollment> findRecentActiveByProgrammeRoot(
            Long tenantId,
            Long programmeRootId,
            int limit
    ) {
        return getEntityManager()
                .createQuery(
                        "select e from CourseEnrollment e join fetch e.user join fetch e.category c "
                                + "where e.tenant.id = :tenantId and c.id = :programmeId "
                                + "and e.enrollmentStatus = 'ACTIVE' and e.status = 'ACTIVE' "
                                + "order by e.enrolledAt desc",
                        CourseEnrollment.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("programmeId", programmeRootId)
                .setMaxResults(limit)
                .getResultList();
    }
}
