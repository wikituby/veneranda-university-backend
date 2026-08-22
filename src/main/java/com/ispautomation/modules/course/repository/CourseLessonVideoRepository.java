package com.ispautomation.modules.course.repository;

import com.ispautomation.modules.course.entity.CourseLessonVideo;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CourseLessonVideoRepository implements PanacheRepositoryBase<CourseLessonVideo, Long> {

    public List<CourseLessonVideo> findByCategoryId(Long categoryId) {
        return list("category.id = ?1 and status = ?2 order by orderIndex asc", categoryId, "ACTIVE");
    }

    public Optional<CourseLessonVideo> findByTenantAndUuid(Long tenantId, UUID uuid) {
        return find("tenant.id = ?1 and uuid = ?2 and status = ?3", tenantId, uuid, "ACTIVE")
                .firstResultOptional();
    }

    public void deleteByCategoryId(Long categoryId) {
        delete("category.id = ?1", categoryId);
    }
}
