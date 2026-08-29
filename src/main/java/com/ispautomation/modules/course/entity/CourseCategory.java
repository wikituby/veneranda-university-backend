package com.ispautomation.modules.course.entity;

import com.ispautomation.common.entity.TenantAwareEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Hierarchical course outline category (adjacency list).
 */
@Entity
@Table(name = "course_categories")
public class CourseCategory extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CourseCategory parent;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    @Column(length = 100)
    private String icon;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "content_id", length = 100)
    private String contentId;

    @Column(name = "content_path", length = 500)
    private String contentPath;

    @Column(name = "google_group_email", length = 255)
    private String googleGroupEmail;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;

    @Column(name = "node_kind", nullable = false, length = 20)
    private String nodeKind = "OUTLINE";

    @Column(name = "price_amount", precision = 12, scale = 2)
    private java.math.BigDecimal priceAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "UGX";

    /** OPEN = free join; REQUEST = creator must accept. */
    @Column(name = "join_mode", nullable = false, length = 20)
    private String joinMode = "OPEN";

    @Column(name = "affiliated_institution", length = 255)
    private String affiliatedInstitution;

    @Column(name = "programme_code", length = 50)
    private String programmeCode;

    @Column(name = "abbreviation", length = 50)
    private String abbreviation;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "created_by_name", length = 200)
    private String createdByName;

    @Column(name = "created_by_avatar_url", length = 500)
    private String createdByAvatarUrl;

    public CourseCategory getParent() {
        return parent;
    }

    public void setParent(CourseCategory parent) {
        this.parent = parent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getJoinMode() {
        return joinMode;
    }

    public void setJoinMode(String joinMode) {
        this.joinMode = joinMode;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByAvatarUrl() {
        return createdByAvatarUrl;
    }

    public void setCreatedByAvatarUrl(String createdByAvatarUrl) {
        this.createdByAvatarUrl = createdByAvatarUrl;
    }
}
