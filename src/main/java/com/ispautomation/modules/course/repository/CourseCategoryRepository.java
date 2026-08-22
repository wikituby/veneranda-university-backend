package com.ispautomation.modules.course.repository;

import com.ispautomation.modules.course.entity.CourseCategory;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CourseCategoryRepository implements PanacheRepositoryBase<CourseCategory, Long> {

    public List<CourseCategory> findByTenant(Long tenantId) {
        return getEntityManager()
                .createQuery(
                        "select c from CourseCategory c left join fetch c.parent "
                                + "where c.tenant.id = :tenantId and c.status = :status "
                                + "order by c.orderIndex asc",
                        CourseCategory.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("status", "ACTIVE")
                .getResultList();
    }

    public List<CourseCategory> findPublishedByTenant(Long tenantId) {
        return getEntityManager()
                .createQuery(
                        "select c from CourseCategory c left join fetch c.parent "
                                + "where c.tenant.id = :tenantId and c.status = :status and c.isPublished = true "
                                + "order by c.orderIndex asc",
                        CourseCategory.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("status", "ACTIVE")
                .getResultList();
    }

    public Optional<CourseCategory> findByTenantAndUuid(Long tenantId, UUID uuid) {
        List<CourseCategory> results = getEntityManager()
                .createQuery(
                        "select c from CourseCategory c left join fetch c.parent "
                                + "where c.tenant.id = :tenantId and c.uuid = :uuid",
                        CourseCategory.class
                )
                .setParameter("tenantId", tenantId)
                .setParameter("uuid", uuid)
                .getResultList();
        return results.stream().findFirst();
    }

    public int nextOrderIndex(Long tenantId, Long parentId) {
        Number max;
        if (parentId == null) {
            max = (Number) getEntityManager()
                    .createQuery(
                            "select coalesce(max(c.orderIndex), 0) from CourseCategory c "
                                    + "where c.tenant.id = :tenantId and c.parent is null"
                    )
                    .setParameter("tenantId", tenantId)
                    .getSingleResult();
        } else {
            max = (Number) getEntityManager()
                    .createQuery(
                            "select coalesce(max(c.orderIndex), 0) from CourseCategory c "
                                    + "where c.tenant.id = :tenantId and c.parent.id = :parentId"
                    )
                    .setParameter("tenantId", tenantId)
                    .setParameter("parentId", parentId)
                    .getSingleResult();
        }
        return max.intValue() + 1;
    }
}
