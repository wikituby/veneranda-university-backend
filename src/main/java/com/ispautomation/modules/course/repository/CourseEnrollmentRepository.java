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
}
