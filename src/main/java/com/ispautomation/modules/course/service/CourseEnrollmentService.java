package com.ispautomation.modules.course.service;

import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.common.exception.NotFoundException;
import com.ispautomation.security.PasswordEncoder;
import com.ispautomation.modules.course.dto.CourseEnrollmentDto;
import com.ispautomation.modules.course.entity.CourseCategory;
import com.ispautomation.modules.course.entity.CourseEnrollment;
import com.ispautomation.modules.course.repository.CourseCategoryRepository;
import com.ispautomation.modules.course.repository.CourseEnrollmentRepository;
import com.ispautomation.modules.rbac.entity.Tenant;
import com.ispautomation.modules.rbac.entity.User;
import com.ispautomation.modules.rbac.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CourseEnrollmentService {

    @Inject
    CourseEnrollmentRepository enrollmentRepository;

    @Inject
    CourseCategoryRepository courseCategoryRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    GoogleGroupsSyncService googleGroupsSyncService;

    @Inject
    CourseSubscriptionService courseSubscriptionService;

    @Inject
    PasswordEncoder passwordEncoder;

    @Transactional
    public CourseEnrollmentDto getStatus(Long tenantId, Long userId, String categoryUuid) {
        CourseCategory root = resolveEnrollmentRoot(tenantId, categoryUuid);
        return enrollmentRepository.findByUserAndCategory(userId, root.getId())
                .filter(e -> "ACTIVE".equals(e.getEnrollmentStatus()))
                .map(CourseEnrollmentDto::fromEntity)
                .orElseGet(() -> CourseEnrollmentDto.notEnrolled(
                        root.getUuid().toString(),
                        root.getTitle(),
                        root.getGoogleGroupEmail()
                ));
    }

    @Transactional
    public List<CourseEnrollmentDto> listMine(Long tenantId, Long userId) {
        return enrollmentRepository.findActiveByUser(tenantId, userId).stream()
                .map(CourseEnrollmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseEnrollmentDto enroll(Long tenantId, Long userId, String categoryUuid) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String memberEmail = blankToNull(user.getEmail());

        CourseCategory root = resolveEnrollmentRoot(tenantId, categoryUuid);
        CourseEnrollment enrollment = enrollmentRepository.findByUserAndCategory(userId, root.getId())
                .orElseGet(() -> {
                    CourseEnrollment created = new CourseEnrollment();
                    created.setUser(user);
                    created.setCategory(root);
                    Tenant tenant = new Tenant();
                    tenant.setId(tenantId);
                    created.setTenant(tenant);
                    created.setCreatedBy(userId);
                    enrollmentRepository.persist(created);
                    return created;
                });

        enrollment.setEnrollmentStatus("ACTIVE");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setUnenrolledAt(null);
        enrollment.setUpdatedBy(userId);
        enrollment.setStatus("ACTIVE");

        if (memberEmail != null) {
            syncAdd(enrollment, root, memberEmail);
        } else {
            enrollment.setGroupSyncStatus("SKIPPED");
            enrollment.setGroupSyncError("User has no email for group sync");
        }
        enrollmentRepository.persist(enrollment);
        return CourseEnrollmentDto.fromEntity(enrollment);
    }

    @Transactional
    public CourseEnrollmentDto unenroll(Long tenantId, Long userId, String categoryUuid, String password) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException(400, "This account has no password. Set a password before you can unjoin.");
        }
        if (password == null || password.isBlank()
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(400, "Incorrect password. Unjoin was not completed.");
        }

        CourseCategory root = resolveEnrollmentRoot(tenantId, categoryUuid);
        courseSubscriptionService.cancelPaidUnderRoot(tenantId, userId, root);
        CourseEnrollment enrollment = enrollmentRepository.findByUserAndCategory(userId, root.getId())
                .orElseThrow(() -> new BusinessException(400, "You are not enrolled in this course."));

        if (!"ACTIVE".equals(enrollment.getEnrollmentStatus())) {
            throw new BusinessException(400, "You are not enrolled in this course.");
        }

        enrollment.setEnrollmentStatus("DROPPED");
        enrollment.setUnenrolledAt(LocalDateTime.now());
        enrollment.setUpdatedBy(userId);

        String memberEmail = blankToNull(user.getEmail());
        if (memberEmail != null) {
            syncRemove(enrollment, root, memberEmail);
        } else {
            enrollment.setGroupSyncStatus("SKIPPED");
            enrollment.setGroupSyncError("User has no email for group removal");
        }

        enrollmentRepository.persist(enrollment);
        return CourseEnrollmentDto.fromEntity(enrollment);
    }

    /**
     * Enrollment is always against the root category (walk parents).
     * Private YouTube access uses that root's google_group_email.
     */
    private CourseCategory resolveEnrollmentRoot(Long tenantId, String categoryUuid) {
        CourseCategory category = findActive(tenantId, categoryUuid);
        CourseCategory current = category;
        int guard = 0;
        while (current.getParent() != null && guard++ < 50) {
            current = current.getParent();
        }
        return current;
    }

    private void syncAdd(CourseEnrollment enrollment, CourseCategory root, String memberEmail) {
        String groupEmail = blankToNull(root.getGoogleGroupEmail());
        if (groupEmail == null) {
            enrollment.setGroupSyncStatus("SKIPPED");
            enrollment.setGroupSyncError("Course has no Google Group email configured");
            enrollment.setGroupSyncedAt(null);
            return;
        }

        GoogleGroupsSyncService.SyncResult result = googleGroupsSyncService.tryAddMember(groupEmail, memberEmail);
        applySyncResult(enrollment, result, "SYNCED");
    }

    private void syncRemove(CourseEnrollment enrollment, CourseCategory root, String memberEmail) {
        String groupEmail = blankToNull(root.getGoogleGroupEmail());
        if (groupEmail == null) {
            enrollment.setGroupSyncStatus("SKIPPED");
            enrollment.setGroupSyncError("Course has no Google Group email configured");
            return;
        }

        GoogleGroupsSyncService.SyncResult result = googleGroupsSyncService.tryRemoveMember(groupEmail, memberEmail);
        applySyncResult(enrollment, result, "REMOVED");
    }

    private void applySyncResult(
            CourseEnrollment enrollment,
            GoogleGroupsSyncService.SyncResult result,
            String successStatus
    ) {
        if (result.success) {
            enrollment.setGroupSyncStatus(successStatus);
            enrollment.setGroupSyncError(null);
            enrollment.setGroupSyncedAt(LocalDateTime.now());
        } else if (result.skipped) {
            enrollment.setGroupSyncStatus("SKIPPED");
            enrollment.setGroupSyncError(result.error);
            enrollment.setGroupSyncedAt(null);
        } else {
            enrollment.setGroupSyncStatus("FAILED");
            enrollment.setGroupSyncError(result.error);
            enrollment.setGroupSyncedAt(null);
        }
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

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
