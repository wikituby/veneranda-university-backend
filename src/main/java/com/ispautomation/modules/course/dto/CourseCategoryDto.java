package com.ispautomation.modules.course.dto;

import com.ispautomation.modules.course.entity.CourseCategory;

import java.time.LocalDateTime;

/**
 * Course category DTO aligned with the frontend CourseCategory model.
 * {@code id} is the category UUID string.
 */
public class CourseCategoryDto {

    private String id;
    private String title;
    private String parentId;
    private Integer orderIndex;
    private String icon;
    private String description;
    private String contentId;
    private String contentPath;
    private String googleGroupEmail;
    private Boolean isPublished;
    private String nodeKind;
    private java.math.BigDecimal priceAmount;
    private String currency;
    private String affiliatedInstitution;
    private String programmeCode;
    private String abbreviation;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CourseCategoryDto() {
    }

    public static CourseCategoryDto fromEntity(CourseCategory entity) {
        CourseCategoryDto dto = new CourseCategoryDto();
        dto.id = entity.getUuid() != null ? entity.getUuid().toString() : null;
        dto.title = entity.getTitle();
        dto.parentId = entity.getParent() != null && entity.getParent().getUuid() != null
                ? entity.getParent().getUuid().toString()
                : null;
        dto.orderIndex = entity.getOrderIndex();
        dto.icon = entity.getIcon();
        dto.description = entity.getDescription();
        dto.contentId = entity.getContentId();
        dto.contentPath = entity.getContentPath();
        dto.googleGroupEmail = entity.getGoogleGroupEmail();
        dto.isPublished = entity.getIsPublished();
        dto.nodeKind = entity.getNodeKind();
        dto.priceAmount = entity.getPriceAmount();
        dto.currency = entity.getCurrency();
        dto.affiliatedInstitution = entity.getAffiliatedInstitution();
        dto.programmeCode = entity.getProgrammeCode();
        dto.abbreviation = entity.getAbbreviation();
        dto.createdBy = entity.getCreatedBy();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public String getGoogleGroupEmail() {
        return googleGroupEmail;
    }

    public void setGoogleGroupEmail(String googleGroupEmail) {
        this.googleGroupEmail = googleGroupEmail;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public String getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }

    public java.math.BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(java.math.BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAffiliatedInstitution() {
        return affiliatedInstitution;
    }

    public void setAffiliatedInstitution(String affiliatedInstitution) {
        this.affiliatedInstitution = affiliatedInstitution;
    }

    public String getProgrammeCode() {
        return programmeCode;
    }

    public void setProgrammeCode(String programmeCode) {
        this.programmeCode = programmeCode;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
