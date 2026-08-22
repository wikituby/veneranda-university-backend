package com.ispautomation.modules.course.repository;

import com.ispautomation.modules.course.entity.CourseLessonContent;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class CourseLessonContentRepository implements PanacheRepositoryBase<CourseLessonContent, Long> {

    public Optional<CourseLessonContent> findByCategoryId(Long categoryId) {
        return find("category.id = ?1 and status = ?2", categoryId, "ACTIVE").firstResultOptional();
    }
}
