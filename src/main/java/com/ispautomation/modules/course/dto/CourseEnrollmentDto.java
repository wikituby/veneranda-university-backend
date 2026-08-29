package com.ispautomation.modules.course.dto;

import com.ispautomation.modules.course.entity.CourseEnrollment;

import java.time.LocalDateTime;

public class CourseEnrollmentDto {

    private String id;
    private String categoryId;
    private String categoryTitle;
    private String googleGroupEmail;
    private String enrollmentStatus;
    private String groupSyncStatus;
    private String groupSyncError;
    private LocalDateTime enrolledAt;
    private LocalDateTime groupSyncedAt;
    private boolean enrolled;
    private String userId;
    private String userFullName;
    private String userEmail;

    public static CourseEnrollmentDto fromEntity(CourseEnrollment entity) {
        CourseEnrollmentDto dto = new CourseEnrollmentDto();
        dto.id = entity.getUuid() != null ? entity.getUuid().toString() : null;
        dto.categoryId = entity.getCategory() != null && entity.getCategory().getUuid() != null
                ? entity.getCategory().getUuid().toString()
                : null;
        dto.categoryTitle = entity.getCategory() != null ? entity.getCategory().getTitle() : null;
        dto.googleGroupEmail = entity.getCategory() != null ? entity.getCategory().getGoogleGroupEmail() : null;
        dto.enrollmentStatus = entity.getEnrollmentStatus();
        dto.groupSyncStatus = entity.getGroupSyncStatus();
        dto.groupSyncError = entity.getGroupSyncError();
        dto.enrolledAt = entity.getEnrolledAt();
        dto.groupSyncedAt = entity.getGroupSyncedAt();
        dto.enrolled = "ACTIVE".equals(entity.getEnrollmentStatus());
        if (entity.getUser() != null) {
            dto.userId = entity.getUser().getUuid() != null ? entity.getUser().getUuid().toString() : null;
            dto.userFullName = entity.getUser().getFullName();
            dto.userEmail = entity.getUser().getEmail();
        }
        return dto;
    }

    public static CourseEnrollmentDto notEnrolled(String categoryId, String categoryTitle, String googleGroupEmail) {
        CourseEnrollmentDto dto = new CourseEnrollmentDto();
        dto.categoryId = categoryId;
        dto.categoryTitle = categoryTitle;
        dto.googleGroupEmail = googleGroupEmail;
        dto.enrollmentStatus = "NONE";
        dto.groupSyncStatus = "SKIPPED";
        dto.enrolled = false;
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryTitle() {
        return categoryTitle;
    }

    public void setCategoryTitle(String categoryTitle) {
        this.categoryTitle = categoryTitle;
    }

    public String getGoogleGroupEmail() {
        return googleGroupEmail;
    }

    public void setGoogleGroupEmail(String googleGroupEmail) {
        this.googleGroupEmail = googleGroupEmail;
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

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDateTime getGroupSyncedAt() {
        return groupSyncedAt;
    }

    public void setGroupSyncedAt(LocalDateTime groupSyncedAt) {
        this.groupSyncedAt = groupSyncedAt;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    public void setEnrolled(boolean enrolled) {
        this.enrolled = enrolled;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
