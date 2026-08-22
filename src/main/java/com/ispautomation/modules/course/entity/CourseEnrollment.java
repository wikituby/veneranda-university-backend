package com.ispautomation.modules.course.entity;

import com.ispautomation.common.entity.TenantAwareEntity;
import com.ispautomation.modules.rbac.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_enrollments")
public class CourseEnrollment extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CourseCategory category;

    @Column(name = "enrollment_status", nullable = false, length = 20)
    private String enrollmentStatus = "ACTIVE";

    @Column(name = "group_sync_status", nullable = false, length = 20)
    private String groupSyncStatus = "PENDING";

    @Column(name = "group_sync_error", columnDefinition = "TEXT")
    private String groupSyncError;

    @Column(name = "group_synced_at")
    private LocalDateTime groupSyncedAt;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "unenrolled_at")
    private LocalDateTime unenrolledAt;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CourseCategory getCategory() {
        return category;
    }

    public void setCategory(CourseCategory category) {
        this.category = category;
    }

    public String getEnrollmentStatus() {
        return enrollmentStatus;
    }

    public void setEnrollmentStatus(String enrollmentStatus) {
        this.enrollmentStatus = enrollmentStatus;
    }

    public String getGroupSyncStatus() {
        return groupSyncStatus;
    }

    public void setGroupSyncStatus(String groupSyncStatus) {
        this.groupSyncStatus = groupSyncStatus;
    }

    public String getGroupSyncError() {
        return groupSyncError;
    }

    public void setGroupSyncError(String groupSyncError) {
        this.groupSyncError = groupSyncError;
    }

    public LocalDateTime getGroupSyncedAt() {
        return groupSyncedAt;
    }

    public void setGroupSyncedAt(LocalDateTime groupSyncedAt) {
        this.groupSyncedAt = groupSyncedAt;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDateTime getUnenrolledAt() {
        return unenrolledAt;
    }

    public void setUnenrolledAt(LocalDateTime unenrolledAt) {
        this.unenrolledAt = unenrolledAt;
    }
}
