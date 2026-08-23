package com.ispautomation.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a course category / outline section.
 */
public class CreateCourseCategoryRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    /** Parent category UUID. Null/blank = top-level section. */
    @Size(max = 36, message = "Parent id must not exceed 36 characters")
    private String parentId;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    private String description;

    @Size(max = 100, message = "Content id must not exceed 100 characters")
    private String contentId;

    @Size(max = 500, message = "Content path must not exceed 500 characters")
    private String contentPath;

    @Size(max = 255, message = "Google group email must not exceed 255 characters")
    private String googleGroupEmail;

    private Boolean isPublished;

    private Integer orderIndex;

    private String nodeKind;

    private java.math.BigDecimal priceAmount;

    private String currency;

    @Size(max = 255, message = "Affiliated institution must not exceed 255 characters")
    private String affiliatedInstitution;

    @Size(max = 50, message = "Programme code must not exceed 50 characters")
    private String programmeCode;

    @Size(max = 50, message = "Abbreviation must not exceed 50 characters")
    private String abbreviation;

    private String coverImageUrl;

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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
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

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
}
