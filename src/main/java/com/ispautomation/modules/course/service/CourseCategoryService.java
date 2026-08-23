package com.ispautomation.modules.course.service;

import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.common.exception.NotFoundException;
import com.ispautomation.modules.course.dto.CoverImageDto;
import com.ispautomation.modules.course.dto.CourseCategoryDto;
import com.ispautomation.modules.course.dto.CreateCourseCategoryRequest;
import com.ispautomation.modules.course.dto.UpdateCourseCategoryRequest;
import com.ispautomation.modules.course.entity.CourseCategory;
import com.ispautomation.modules.course.repository.CourseCategoryRepository;
import com.ispautomation.modules.rbac.entity.Tenant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CourseCategoryService {

    private static final String PROVIDER_R2 = "r2";
    private static final long MAX_COVER_BYTES = 5_000_000L;

    @Inject
    CourseCategoryRepository courseCategoryRepository;

    @Inject
    GoogleGroupsSyncService googleGroupsSyncService;

    @Inject
    R2StorageService r2StorageService;

    @Transactional
    public List<CourseCategoryDto> listCategories(Long tenantId, boolean publishedOnly) {
        List<CourseCategory> categories = publishedOnly
                ? courseCategoryRepository.findPublishedByTenant(tenantId)
                : courseCategoryRepository.findByTenant(tenantId);
        return categories.stream().map(this::toDto).collect(Collectors.toList());
    }

    public CourseCategoryDto getByUuid(Long tenantId, String uuid) {
        return toDto(findActive(tenantId, uuid));
    }

    @Transactional
    public CourseCategoryDto create(CreateCourseCategoryRequest request, Long tenantId, Long createdBy) {
        CourseCategory parent = null;
        Long parentDbId = null;

        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            parent = findActive(tenantId, request.getParentId());
            parentDbId = parent.getId();
        }

        CourseCategory category = new CourseCategory();
        category.setTitle(request.getTitle().trim());
        category.setDescription(blankToNull(request.getDescription()));
        category.setIcon(blankToNull(request.getIcon()) != null
                ? request.getIcon().trim()
                : (parent == null ? "folder" : "folder_open"));
        category.setContentId(blankToNull(request.getContentId()));
        category.setContentPath(blankToNull(request.getContentPath()));
        category.setGoogleGroupEmail(blankToNull(request.getGoogleGroupEmail()));
        category.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : true);
        category.setNodeKind(resolveNodeKind(request.getNodeKind(), parent));
        category.setPriceAmount(request.getPriceAmount());
        category.setCurrency(blankToNull(request.getCurrency()) != null ? request.getCurrency().trim().toUpperCase() : "KES");
        category.setAffiliatedInstitution(blankToNull(request.getAffiliatedInstitution()));
        category.setProgrammeCode(blankToNull(request.getProgrammeCode()));
        category.setAbbreviation(blankToNull(request.getAbbreviation()));
        category.setCoverImageUrl(sanitizeCoverInput(request.getCoverImageUrl()));
        category.setOrderIndex(
                request.getOrderIndex() != null
                        ? request.getOrderIndex()
                        : courseCategoryRepository.nextOrderIndex(tenantId, parentDbId)
        );
        category.setParent(parent);
        category.setStatus("ACTIVE");
        category.setCreatedBy(createdBy);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        category.setTenant(tenant);

        courseCategoryRepository.persist(category);

        if (parent == null && category.getGoogleGroupEmail() == null
                && googleGroupsSyncService.isAutoCreateGroupOnRootEnabled()) {
            String generatedGroupEmail = googleGroupsSyncService.createGroupForCourse(category.getTitle());
            if (generatedGroupEmail != null && !generatedGroupEmail.isBlank()) {
                category.setGoogleGroupEmail(generatedGroupEmail);
                courseCategoryRepository.persist(category);
            }
        }

        return toDto(category);
    }

    @Transactional
    public CourseCategoryDto uploadCoverImage(
            Long tenantId,
            String uuid,
            Long userId,
            String filename,
            String contentType,
            long contentLength,
            InputStream data
    ) {
        r2StorageService.requireEnabled();
        if (contentLength <= 0) {
            throw new BusinessException(400, "Empty image file.");
        }
        if (contentLength > MAX_COVER_BYTES) {
            throw new BusinessException(400, "Image exceeds 5 MB limit.");
        }

        CourseCategory category = findActive(tenantId, uuid);
        requireProgrammeNode(category);

        String ext = R2StorageService.extensionForContentType(contentType, filename);
        String objectKey = String.format(
                Locale.ROOT,
                "tenants/%d/programmes/%s/cover.%s",
                tenantId,
                category.getUuid(),
                ext
        );

        deleteStoredCover(category.getCoverImageUrl());
        r2StorageService.putObject(objectKey, data, contentLength, contentType, R2StorageService.MediaKind.IMAGE);
        category.setCoverImageUrl(objectKey);
        category.setUpdatedBy(userId);
        courseCategoryRepository.persist(category);
        return toDto(category);
    }

    public CoverImageDto getCoverImageUrl(Long tenantId, String uuid) {
        CourseCategory category = findActive(tenantId, uuid);
        String stored = category.getCoverImageUrl();
        if (!isR2ObjectKey(stored)) {
            throw new BusinessException(404, "This programme has no uploaded cover image.");
        }

        R2StorageService.PresignedPlayback playback = r2StorageService.presignGet(stored);
        CoverImageDto dto = new CoverImageDto();
        dto.setCategoryId(category.getUuid().toString());
        dto.setUrl(playback.url());
        dto.setExpiresAt(playback.expiresAt());
        dto.setProvider(PROVIDER_R2);
        return dto;
    }

    public boolean isR2Enabled() {
        return r2StorageService.isEnabled();
    }

    @Transactional
    public CourseCategoryDto update(Long tenantId, String uuid, UpdateCourseCategoryRequest request, Long updatedBy) {
        CourseCategory category = findActive(tenantId, uuid);

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new BusinessException(400, "Title cannot be blank");
            }
            category.setTitle(title);
        }
        if (request.getDescription() != null) {
            category.setDescription(blankToNull(request.getDescription()));
        }
        if (request.getIcon() != null) {
            category.setIcon(blankToNull(request.getIcon()));
        }
        if (request.getContentId() != null) {
            category.setContentId(blankToNull(request.getContentId()));
        }
        if (request.getContentPath() != null) {
            category.setContentPath(blankToNull(request.getContentPath()));
        }
        if (request.getGoogleGroupEmail() != null) {
            category.setGoogleGroupEmail(blankToNull(request.getGoogleGroupEmail()));
        }
        if (request.getIsPublished() != null) {
            category.setIsPublished(request.getIsPublished());
        }
        if (request.getOrderIndex() != null) {
            category.setOrderIndex(request.getOrderIndex());
        }
        if (request.getNodeKind() != null && !request.getNodeKind().isBlank()) {
            category.setNodeKind(request.getNodeKind().trim().toUpperCase());
        }
        if (request.getPriceAmount() != null) {
            category.setPriceAmount(request.getPriceAmount());
        }
        if (request.getCurrency() != null) {
            category.setCurrency(blankToNull(request.getCurrency()) != null
                    ? request.getCurrency().trim().toUpperCase()
                    : "KES");
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        if (request.getAffiliatedInstitution() != null) {
            category.setAffiliatedInstitution(blankToNull(request.getAffiliatedInstitution()));
        }
        if (request.getProgrammeCode() != null) {
            category.setProgrammeCode(blankToNull(request.getProgrammeCode()));
        }
        if (request.getAbbreviation() != null) {
            category.setAbbreviation(blankToNull(request.getAbbreviation()));
        }
        if (request.getCoverImageUrl() != null) {
            String nextCover = sanitizeCoverInput(request.getCoverImageUrl());
            if (!java.util.Objects.equals(nextCover, category.getCoverImageUrl())) {
                deleteStoredCover(category.getCoverImageUrl());
                category.setCoverImageUrl(nextCover);
            }
        }
        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            applyParentChange(tenantId, category, request.getParentId().trim());
        }

        category.setUpdatedBy(updatedBy);
        courseCategoryRepository.persist(category);
        return toDto(category);
    }

    @Transactional
    public void delete(Long tenantId, String uuid) {
        CourseCategory category = findActive(tenantId, uuid);
        courseCategoryRepository.delete(category);
    }

    public void addGroupMember(Long tenantId, String uuid, String email) {
        CourseCategory category = findActive(tenantId, uuid);
        String groupEmail = blankToNull(category.getGoogleGroupEmail());
        if (groupEmail == null) {
            throw new BusinessException(400, "This section has no Google Group email configured.");
        }
        googleGroupsSyncService.addMember(groupEmail, email.trim());
    }

    public void removeGroupMember(Long tenantId, String uuid, String email) {
        CourseCategory category = findActive(tenantId, uuid);
        String groupEmail = blankToNull(category.getGoogleGroupEmail());
        if (groupEmail == null) {
            throw new BusinessException(400, "This section has no Google Group email configured.");
        }
        googleGroupsSyncService.removeMember(groupEmail, email.trim());
    }

    public boolean canEdit(Long tenantId, String uuid, Long userId) {
        CourseCategory category = findActive(tenantId, uuid);
        CourseCategory root = category;
        int guard = 0;
        while (root.getParent() != null && guard++ < 50) {
            root = root.getParent();
        }
        return root.getCreatedBy() != null && root.getCreatedBy().equals(userId);
    }

    public CourseCategory getEntity(Long tenantId, String uuid) {
        return findActive(tenantId, uuid);
    }

    private void applyParentChange(Long tenantId, CourseCategory category, String parentUuidText) {
        CourseCategory newParent = findActive(tenantId, parentUuidText);
        if (newParent.getUuid() != null && newParent.getUuid().equals(category.getUuid())) {
            throw new BusinessException(400, "An item cannot be its own parent.");
        }

        String parentKind = newParent.getNodeKind() != null ? newParent.getNodeKind() : "OUTLINE";
        if (!"UNIT".equals(parentKind) && !"OUTLINE".equals(parentKind)) {
            throw new BusinessException(400, "Parent must be the course unit or another outline item.");
        }

        if (isAncestorOf(category, newParent)) {
            throw new BusinessException(400, "Cannot move an item under one of its descendants.");
        }

        CourseCategory currentUnit = owningUnit(category);
        CourseCategory newUnit = "UNIT".equals(parentKind) ? newParent : owningUnit(newParent);
        if (currentUnit != null && newUnit != null && !currentUnit.getUuid().equals(newUnit.getUuid())) {
            throw new BusinessException(400, "Parent must stay inside the same course unit.");
        }

        CourseCategory currentParent = category.getParent();
        boolean sameParent = currentParent != null
                && currentParent.getUuid() != null
                && currentParent.getUuid().equals(newParent.getUuid());
        if (sameParent) {
            return;
        }

        category.setParent(newParent);
        category.setOrderIndex(courseCategoryRepository.nextOrderIndex(tenantId, newParent.getId()));
    }

    /** True when {@code ancestor} appears above {@code node} in the parent chain. */
    private static boolean isAncestorOf(CourseCategory ancestor, CourseCategory node) {
        CourseCategory cursor = node.getParent();
        int guard = 0;
        while (cursor != null && guard++ < 80) {
            if (ancestor.getUuid() != null && ancestor.getUuid().equals(cursor.getUuid())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private static CourseCategory owningUnit(CourseCategory start) {
        CourseCategory cursor = start;
        int guard = 0;
        while (cursor != null && guard++ < 80) {
            if ("UNIT".equals(cursor.getNodeKind())) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private static String resolveNodeKind(String requested, CourseCategory parent) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim().toUpperCase();
        }
        if (parent == null) {
            return "PROGRAMME";
        }
        String parentKind = parent.getNodeKind() != null ? parent.getNodeKind() : "OUTLINE";
        return switch (parentKind) {
            case "PROGRAMME" -> "YEAR";
            case "YEAR" -> "SEMESTER";
            case "SEMESTER" -> "UNIT";
            default -> "OUTLINE";
        };
    }

    private CourseCategory findActive(Long tenantId, String uuidText) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Course category not found: " + uuidText);
        }

        CourseCategory category = courseCategoryRepository
                .findByTenantAndUuid(tenantId, uuid)
                .orElseThrow(() -> new NotFoundException("Course category not found: " + uuidText));

        if (!"ACTIVE".equals(category.getStatus())) {
            throw new NotFoundException("Course category not found: " + uuidText);
        }
        return category;
    }

    private CourseCategoryDto toDto(CourseCategory entity) {
        CourseCategoryDto dto = CourseCategoryDto.fromEntity(entity);
        dto.setCoverImageUrl(resolveCoverDisplayUrl(entity.getCoverImageUrl()));
        return dto;
    }

    private String resolveCoverDisplayUrl(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (isExternalCoverUrl(stored)) {
            return stored;
        }
        if (isR2ObjectKey(stored) && r2StorageService.isEnabled()) {
            return r2StorageService.presignGet(stored).url();
        }
        return stored;
    }

    private static String sanitizeCoverInput(String value) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.startsWith("data:")) {
            throw new BusinessException(400, "Upload cover images using the cover upload endpoint instead of inline data.");
        }
        return trimmed;
    }

    private void deleteStoredCover(String stored) {
        if (isR2ObjectKey(stored)) {
            r2StorageService.deleteObject(stored);
        }
    }

    private static boolean isR2ObjectKey(String value) {
        return value != null && value.startsWith("tenants/");
    }

    private static boolean isExternalCoverUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static void requireProgrammeNode(CourseCategory category) {
        String kind = category.getNodeKind() != null ? category.getNodeKind() : "PROGRAMME";
        if (!"PROGRAMME".equals(kind)) {
            throw new BusinessException(400, "Cover images are only supported for programmes.");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
