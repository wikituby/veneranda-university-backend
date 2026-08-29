package com.ispautomation.modules.course.service;

import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.common.exception.NotFoundException;
import com.ispautomation.modules.course.dto.CourseAccessDto;
import com.ispautomation.modules.course.dto.CourseSubscriptionDto;
import com.ispautomation.modules.course.entity.CourseCategory;
import com.ispautomation.modules.course.entity.CourseSubscription;
import com.ispautomation.modules.course.repository.CourseCategoryRepository;
import com.ispautomation.modules.course.repository.CourseEnrollmentRepository;
import com.ispautomation.modules.course.repository.CourseSubscriptionRepository;
import com.ispautomation.modules.rbac.entity.Tenant;
import com.ispautomation.modules.rbac.entity.User;
import com.ispautomation.modules.rbac.repository.UserRepository;
import com.ispautomation.security.PasswordEncoder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CourseSubscriptionService {

    @Inject
    CourseSubscriptionRepository subscriptionRepository;

    @Inject
    CourseCategoryRepository courseCategoryRepository;

    @Inject
    CourseEnrollmentRepository enrollmentRepository;

    @Inject
    LmsPricingConfig lmsPricingConfig;

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    @Transactional
    public List<CourseSubscriptionDto> listMine(Long tenantId, Long userId) {
        return subscriptionRepository.findByUser(tenantId, userId).stream()
                .map(CourseSubscriptionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseAccessDto getAccess(Long tenantId, Long userId, String categoryUuid, boolean manager) {
        CourseCategory category = findActive(tenantId, categoryUuid);
        CourseCategory root = walkToRoot(category);
        boolean enrolled = enrollmentRepository.findByUserAndCategory(userId, root.getId())
                .filter(e -> "ACTIVE".equals(e.getEnrollmentStatus()))
                .isPresent();
        boolean paid = hasPaidAccess(tenantId, userId, category);
        boolean owns = root.getCreatedBy() != null && root.getCreatedBy().equals(userId);

        CourseAccessDto dto = new CourseAccessDto();
        dto.setCategoryId(category.getUuid().toString());
        dto.setCategoryTitle(category.getTitle());
        dto.setNodeKind(category.getNodeKind());
        dto.setEnrolled(enrolled);
        dto.setPaid(paid);
        dto.setCanManage(manager || owns);
        dto.setCanAccess(manager || paid);
        dto.setAmount(resolveTotalPrice(category));
        dto.setCurrency(category.getCurrency() != null ? category.getCurrency() : "KES");
        return dto;
    }

    public boolean hasPaidAccess(Long tenantId, Long userId, CourseCategory category) {
        Set<Long> paidIds = new HashSet<>();
        for (CourseSubscription sub : subscriptionRepository.findPaidByUser(tenantId, userId)) {
            if (sub.getCategory() != null) {
                paidIds.add(sub.getCategory().getId());
            }
        }
        CourseCategory current = category;
        int guard = 0;
        while (current != null && guard++ < 50) {
            if (paidIds.contains(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    @Transactional
    public boolean canViewContent(Long tenantId, Long userId, CourseCategory category, boolean manager) {
        if (manager) {
            return true;
        }
        return hasPaidAccess(tenantId, userId, category);
    }

    @Transactional
    public CourseSubscriptionDto checkout(Long tenantId, Long userId, String categoryUuid, boolean trial) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        CourseCategory category = findActive(tenantId, categoryUuid);
        String kind = category.getNodeKind();
        if (!"UNIT".equals(kind) && !"SEMESTER".equals(kind) && !"YEAR".equals(kind) && !"PROGRAMME".equals(kind)) {
            throw new BusinessException(400, "Choose a course unit, semester, year, or the whole programme to subscribe.");
        }

        CourseCategory root = walkToRoot(category);
        boolean enrolled = enrollmentRepository.findByUserAndCategory(userId, root.getId())
                .filter(e -> "ACTIVE".equals(e.getEnrollmentStatus()))
                .isPresent();
        if (!enrolled) {
            throw new BusinessException(400, "Join the programme before paying for a semester or unit.");
        }

        CourseCategory target = category;
        BigDecimal coordinatorShare = trial ? BigDecimal.ZERO : resolveCoordinatorShare(target);
        BigDecimal serverFee = trial ? BigDecimal.ZERO : lmsPricingConfig.serverFeeAmount();
        BigDecimal amount = coordinatorShare.add(serverFee);
        CourseSubscription subscription = subscriptionRepository.findByUserAndCategory(userId, target.getId())
                .orElseGet(() -> {
                    CourseSubscription created = new CourseSubscription();
                    created.setUser(user);
                    created.setCategory(target);
                    Tenant tenant = new Tenant();
                    tenant.setId(tenantId);
                    created.setTenant(tenant);
                    created.setCreatedBy(userId);
                    subscriptionRepository.persist(created);
                    return created;
                });

        boolean alreadyPaidForever = "PAID".equals(subscription.getPaymentStatus())
                && subscription.getExpiresAt() == null
                && "ACTIVE".equals(subscription.getStatus());
        if (alreadyPaidForever) {
            throw new BusinessException(400, "You are already subscribed to this section.");
        }

        subscription.setAmount(amount);
        subscription.setCoordinatorAmount(coordinatorShare);
        subscription.setServerFeeAmount(serverFee);
        subscription.setCurrency(target.getCurrency() != null ? target.getCurrency() : lmsPricingConfig.defaultCurrency());
        subscription.setPaymentMethod(trial ? "TRIAL" : "SIMULATED");
        subscription.setPaymentStatus("PAID");
        subscription.setPaidAt(LocalDateTime.now());
        subscription.setExpiresAt(trial ? LocalDateTime.now().plusHours(48) : null);
        subscription.setStatus("ACTIVE");
        subscription.setUpdatedBy(userId);
        subscriptionRepository.persist(subscription);
        return CourseSubscriptionDto.fromEntity(subscription);
    }

    @Transactional
    public CourseSubscriptionDto unsubscribe(Long tenantId, Long userId, String categoryUuid, String password) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException(400, "This account has no password. Set a password before you can unsubscribe.");
        }
        if (password == null || password.isBlank()
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(400, "Incorrect password. Unsubscribe was not completed.");
        }

        CourseCategory category = findActive(tenantId, categoryUuid);
        CourseSubscription subscription = subscriptionRepository.findByUserAndCategory(userId, category.getId())
                .orElseThrow(() -> new BusinessException(400, "You are not subscribed to this section."));
        if (!"PAID".equals(subscription.getPaymentStatus()) || !"ACTIVE".equals(subscription.getStatus())) {
            throw new BusinessException(400, "You are not subscribed to this section.");
        }
        subscription.setPaymentStatus("FAILED");
        subscription.setUpdatedBy(userId);
        if (subscription.getExpiresAt() == null || subscription.getExpiresAt().isAfter(LocalDateTime.now())) {
            subscription.setExpiresAt(LocalDateTime.now());
        }
        subscriptionRepository.persist(subscription);
        return CourseSubscriptionDto.fromEntity(subscription);
    }

    public void cancelPaidUnderRoot(Long tenantId, Long userId, CourseCategory root) {
        if (root == null) {
            return;
        }
        Long rootId = root.getId();
        for (CourseSubscription sub : subscriptionRepository.findPaidByUser(tenantId, userId)) {
            CourseCategory current = sub.getCategory();
            int guard = 0;
            boolean underRoot = false;
            while (current != null && guard++ < 50) {
                if (rootId.equals(current.getId())) {
                    underRoot = true;
                    break;
                }
                current = current.getParent();
            }
            if (!underRoot) {
                continue;
            }
            sub.setPaymentStatus("FAILED");
            sub.setUpdatedBy(userId);
            if (sub.getExpiresAt() == null || sub.getExpiresAt().isAfter(LocalDateTime.now())) {
                sub.setExpiresAt(LocalDateTime.now());
            }
            subscriptionRepository.persist(sub);
        }
    }

    public boolean hasPaidUnderRoot(Long tenantId, Long userId, CourseCategory root) {
        if (root == null) {
            return false;
        }
        Long rootId = root.getId();
        for (CourseSubscription sub : subscriptionRepository.findPaidByUser(tenantId, userId)) {
            CourseCategory current = sub.getCategory();
            int guard = 0;
            while (current != null && guard++ < 50) {
                if (rootId.equals(current.getId())) {
                    return true;
                }
                current = current.getParent();
            }
        }
        return false;
    }

    /** Public total charged to the learner (coordinator share + server fee). */
    public BigDecimal resolveTotalPrice(CourseCategory category) {
        return resolveCoordinatorShare(category).add(lmsPricingConfig.serverFeeAmount());
    }

    private BigDecimal resolveCoordinatorShare(CourseCategory category) {
        if (category.getPriceAmount() != null) {
            return category.getPriceAmount();
        }
        if ("PROGRAMME".equals(category.getNodeKind())) {
            return new BigDecimal("250000");
        }
        if ("YEAR".equals(category.getNodeKind())) {
            return new BigDecimal("80000");
        }
        if ("SEMESTER".equals(category.getNodeKind())) {
            return new BigDecimal("40000");
        }
        if ("UNIT".equals(category.getNodeKind())) {
            return new BigDecimal("15000");
        }
        return BigDecimal.ZERO;
    }

    private CourseCategory walkToRoot(CourseCategory category) {
        CourseCategory current = category;
        int guard = 0;
        while (current.getParent() != null && guard++ < 50) {
            current = current.getParent();
        }
        return current;
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
}
