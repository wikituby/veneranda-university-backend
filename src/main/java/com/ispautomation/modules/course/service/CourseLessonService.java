package com.ispautomation.modules.course.service;

import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.common.exception.ForbiddenException;
import com.ispautomation.common.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ispautomation.modules.course.dto.CourseLessonDto;
import com.ispautomation.modules.course.dto.DocumentViewDto;
import com.ispautomation.modules.course.dto.LessonDocumentDto;
import com.ispautomation.modules.course.dto.LessonLiveSessionDto;
import com.ispautomation.modules.course.dto.LessonSlideDto;
import com.ispautomation.modules.course.dto.LessonVideoDto;
import com.ispautomation.modules.course.dto.SaveCourseLessonRequest;
import com.ispautomation.modules.course.dto.SlideViewDto;
import com.ispautomation.modules.course.dto.VideoPlaybackDto;
import com.ispautomation.modules.course.entity.CourseCategory;
import com.ispautomation.modules.course.entity.CourseEnrollment;
import com.ispautomation.modules.course.entity.CourseLessonContent;
import com.ispautomation.modules.course.entity.CourseLessonDocument;
import com.ispautomation.modules.course.entity.CourseLessonLiveSession;
import com.ispautomation.modules.course.entity.CourseLessonSlide;
import com.ispautomation.modules.course.entity.CourseLessonVideo;
import com.ispautomation.modules.course.repository.CourseCategoryRepository;
import com.ispautomation.modules.course.repository.CourseEnrollmentRepository;
import com.ispautomation.modules.course.repository.CourseLessonContentRepository;
import com.ispautomation.modules.course.repository.CourseLessonDocumentRepository;
import com.ispautomation.modules.course.repository.CourseLessonLiveSessionRepository;
import com.ispautomation.modules.course.repository.CourseLessonSlideRepository;
import com.ispautomation.modules.course.repository.CourseLessonVideoRepository;
import com.ispautomation.modules.rbac.entity.Tenant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CourseLessonService {

    private static final String PROVIDER_R2 = "r2";
    /** Staff edits stay in draft JSON until Publish copies them to live media tables. */

    @Inject
    CourseCategoryRepository courseCategoryRepository;

    @Inject
    CourseLessonContentRepository lessonContentRepository;

    @Inject
    CourseLessonSlideRepository lessonSlideRepository;

    @Inject
    CourseLessonVideoRepository lessonVideoRepository;

    @Inject
    CourseLessonDocumentRepository lessonDocumentRepository;

    @Inject
    CourseLessonLiveSessionRepository lessonLiveSessionRepository;

    @Inject
    CourseEnrollmentRepository enrollmentRepository;

    @Inject
    CourseSubscriptionService courseSubscriptionService;

    @Inject
    R2StorageService r2StorageService;

    @Inject
    ObjectMapper objectMapper;

    public CourseLessonDto getLesson(Long tenantId, String categoryUuid) {
        return getLesson(tenantId, categoryUuid, false);
    }

    public CourseLessonDto getLesson(Long tenantId, String categoryUuid, boolean forEditor) {
        CourseCategory category = findCategory(tenantId, categoryUuid);
        CourseLessonDto dto = new CourseLessonDto();
        dto.setCategoryId(category.getUuid().toString());
        dto.setTitle(category.getTitle());
        dto.setDescription(category.getDescription());

        lessonContentRepository.findByCategoryId(category.getId()).ifPresent(content -> {
            applyNotesForReader(dto, content, forEditor);
        });

        CourseLessonContent content = lessonContentRepository.findByCategoryId(category.getId()).orElse(null);
        if (forEditor && content != null && hasMediaDraft(content)) {
            dto.setSlides(toClientSlides(readDraftSlides(content)));
            dto.setVideos(toClientVideos(readDraftVideos(content)));
            dto.setDocuments(toClientDocuments(readDraftDocuments(content)));
            dto.setLiveSessions(readDraftLiveSessions(content));
        } else {
            dto.setSlides(
                    lessonSlideRepository.findByCategoryId(category.getId()).stream()
                            .map(this::toSlideDto)
                            .collect(Collectors.toList())
            );
            dto.setVideos(
                    lessonVideoRepository.findByCategoryId(category.getId()).stream()
                            .map(this::toVideoDto)
                            .collect(Collectors.toList())
            );
            dto.setDocuments(
                    lessonDocumentRepository.findByCategoryId(category.getId()).stream()
                            .map(this::toDocumentDto)
                            .collect(Collectors.toList())
            );
            dto.setLiveSessions(
                    lessonLiveSessionRepository.findByCategoryId(category.getId()).stream()
                            .map(this::toLiveSessionDto)
                            .collect(Collectors.toList())
            );
        }
        if (forEditor && content != null) {
            dto.setHasUnpublishedNotes(dto.getHasUnpublishedNotes() || hasUnpublishedMedia(content, category.getId()));
        }
        return dto;
    }

    @Transactional
    public CourseLessonDto saveLesson(Long tenantId, String categoryUuid, SaveCourseLessonRequest request, Long userId) {
        CourseCategory category = findCategory(tenantId, categoryUuid);

        CourseLessonContent content = lessonContentRepository
                .findByCategoryId(category.getId())
                .orElseGet(() -> {
                    CourseLessonContent created = new CourseLessonContent();
                    created.setCategory(category);
                    created.setStatus("ACTIVE");
                    created.setCreatedBy(userId);
                    Tenant tenant = new Tenant();
                    tenant.setId(tenantId);
                    created.setTenant(tenant);
                    return created;
                });

        content.setNotesDraftBody(request.getNotesBody());
        storeMediaDrafts(content, category, request);
        content.setUpdatedBy(userId);
        lessonContentRepository.persist(content);

        return getLesson(tenantId, categoryUuid, true);
    }

    @Transactional
    public CourseLessonDto publishLesson(Long tenantId, String categoryUuid, Long userId) {
        CourseCategory category = findCategory(tenantId, categoryUuid);
        CourseLessonContent content = lessonContentRepository
                .findByCategoryId(category.getId())
                .orElseThrow(() -> new NotFoundException("No lesson notes to publish"));
        String draft = content.getNotesDraftBody() != null
                ? content.getNotesDraftBody()
                : content.getNotesBody();
        content.setNotesBody(draft);
        content.setNotesDraftBody(draft);
        content.setUpdatedBy(userId);
        if (!hasMediaDraft(content)) {
            seedMediaDraftFromPublished(content, category.getId());
        }
        lessonContentRepository.persist(content);
        replacePublishedMedia(
                tenantId,
                category,
                userId,
                readDraftSlides(content),
                readDraftVideos(content),
                readDraftDocuments(content),
                readDraftLiveSessions(content)
        );
        return getLesson(tenantId, categoryUuid, true);
    }

    @Transactional
    public LessonVideoDto uploadR2Video(
            Long tenantId,
            String categoryUuid,
            Long userId,
            String title,
            String filename,
            String contentType,
            long contentLength,
            InputStream data
    ) {
        r2StorageService.requireEnabled();
        if (contentLength <= 0) {
            throw new BusinessException(400, "Empty video file.");
        }
        // Soft cap aligned with free-tier reality (~1.5 GB per file)
        if (contentLength > 1_500_000_000L) {
            throw new BusinessException(400, "Video exceeds 1.5 GB limit.");
        }

        CourseCategory category = findCategory(tenantId, categoryUuid);
        UUID videoUuid = UUID.randomUUID();
        String ext = R2StorageService.extensionForContentType(contentType, filename);
        String objectKey = String.format(
                Locale.ROOT,
                "tenants/%d/lessons/%s/%s.%s",
                tenantId,
                category.getUuid(),
                videoUuid,
                ext
        );

        r2StorageService.putObject(objectKey, data, contentLength, contentType);

        CourseLessonContent content = ensureLessonContent(category, tenantId, userId);
        if (!hasMediaDraft(content)) {
            seedMediaDraftFromPublished(content, category.getId());
        }
        List<LessonVideoDto> videos = readDraftVideos(content);
        LessonVideoDto stored = new LessonVideoDto();
        stored.setId(videoUuid.toString());
        stored.setTitle(safeTitle(title, filename != null && !filename.isBlank() ? filename : "Uploaded video"));
        stored.setUrl(objectKey);
        stored.setProvider(PROVIDER_R2);
        stored.setOrderIndex(videos.size());
        stored.setSignedPlayback(true);
        videos.add(stored);
        content.setVideosDraftJson(writeJson(videos));
        content.setUpdatedBy(userId);
        lessonContentRepository.persist(content);
        return toClientVideo(stored);
    }

    @Transactional
    public LessonSlideDto uploadR2Slide(
            Long tenantId,
            String categoryUuid,
            Long userId,
            String title,
            String filename,
            String contentType,
            long contentLength,
            InputStream data
    ) {
        r2StorageService.requireEnabled();
        if (contentLength <= 0) {
            throw new BusinessException(400, "Empty slide file.");
        }
        if (contentLength > 200_000_000L) {
            throw new BusinessException(400, "Slide file exceeds 200 MB limit.");
        }

        CourseCategory category = findCategory(tenantId, categoryUuid);
        UUID slideUuid = UUID.randomUUID();
        String ext = R2StorageService.extensionForContentType(contentType, filename);
        if (!ext.matches("(?i)pdf|ppt|pptx|ppsx")) {
            // Force extension from filename when content-type is generic
            String fromName = R2StorageService.extensionForContentType(null, filename);
            if (fromName.matches("(?i)pdf|ppt|pptx|ppsx")) {
                ext = fromName;
            } else {
                throw new BusinessException(400, "Upload a PDF or PowerPoint file (.pdf, .ppt, .pptx).");
            }
        }

        String objectKey = String.format(
                Locale.ROOT,
                "tenants/%d/lessons/%s/slides/%s.%s",
                tenantId,
                category.getUuid(),
                slideUuid,
                ext
        );

        r2StorageService.putObject(
                objectKey,
                data,
                contentLength,
                contentType,
                R2StorageService.MediaKind.DOCUMENT
        );

        CourseLessonContent content = ensureLessonContent(category, tenantId, userId);
        if (!hasMediaDraft(content)) {
            seedMediaDraftFromPublished(content, category.getId());
        }
        List<LessonSlideDto> slides = readDraftSlides(content);
        LessonSlideDto stored = new LessonSlideDto();
        stored.setId(slideUuid.toString());
        stored.setTitle(safeTitle(title, filename != null && !filename.isBlank() ? filename : "Uploaded slides"));
        stored.setUrl(objectKey);
        stored.setProvider(PROVIDER_R2);
        stored.setOrderIndex(slides.size());
        stored.setSignedPlayback(true);
        slides.add(stored);
        content.setSlidesDraftJson(writeJson(slides));
        content.setUpdatedBy(userId);
        lessonContentRepository.persist(content);
        return toClientSlide(stored);
    }

    @Transactional
    public SlideViewDto getSlideViewUrl(
            Long tenantId,
            String categoryUuid,
            String slideUuid,
            Long userId,
            boolean canManage
    ) {
        CourseCategory category = findCategory(tenantId, categoryUuid);
        String objectKey = resolveSlideObjectKey(tenantId, category, slideUuid, canManage);
        if (!canManage) {
            requirePaidAccess(tenantId, userId, category, false);
        }

        R2StorageService.PresignedPlayback signed = r2StorageService.presignGet(objectKey);
        SlideViewDto dto = new SlideViewDto();
        dto.setSlideId(slideUuid);
        dto.setUrl(signed.url());
        dto.setExpiresAt(signed.expiresAt());
        dto.setProvider(PROVIDER_R2);
        dto.setFormat(detectSlideFormat(objectKey));
        return dto;
    }

    @Transactional
    public LessonDocumentDto uploadR2Document(
            Long tenantId,
            String categoryUuid,
            Long userId,
            String title,
            String filename,
            String contentType,
            long contentLength,
            InputStream data
    ) {
        r2StorageService.requireEnabled();
        if (contentLength <= 0) {
            throw new BusinessException(400, "Empty document file.");
        }
        if (contentLength > 100_000_000L) {
            throw new BusinessException(400, "Document exceeds 100 MB limit.");
        }

        CourseCategory category = findCategory(tenantId, categoryUuid);
        UUID documentUuid = UUID.randomUUID();
        String ext = R2StorageService.extensionForContentType(contentType, filename);
        if (!ext.matches("(?i)pdf|doc|docx|ppt|pptx|ppsx|xls|xlsx|odt|odp|rtf|txt|csv")) {
            String fromName = R2StorageService.extensionForContentType(null, filename);
            if (fromName.matches("(?i)pdf|doc|docx|ppt|pptx|ppsx|xls|xlsx|odt|odp|rtf|txt|csv")) {
                ext = fromName;
            } else {
                throw new BusinessException(400,
                        "Upload a supported document (PDF, Word, Excel, PowerPoint, TXT, CSV, ODT/ODP, RTF).");
            }
        }

        String objectKey = String.format(
                Locale.ROOT,
                "tenants/%d/lessons/%s/documents/%s.%s",
                tenantId,
                category.getUuid(),
                documentUuid,
                ext
        );

        r2StorageService.putObject(
                objectKey,
                data,
                contentLength,
                contentType,
                R2StorageService.MediaKind.DOCUMENT
        );

        CourseLessonContent content = ensureLessonContent(category, tenantId, userId);
        if (!hasMediaDraft(content)) {
            seedMediaDraftFromPublished(content, category.getId());
        }
        List<LessonDocumentDto> documents = readDraftDocuments(content);
        LessonDocumentDto stored = new LessonDocumentDto();
        stored.setId(documentUuid.toString());
        stored.setTitle(safeTitle(title, filename != null && !filename.isBlank() ? filename : "Uploaded document"));
        stored.setUrl(objectKey);
        stored.setProvider(PROVIDER_R2);
        stored.setFileFormat(detectDocumentFormat(objectKey));
        stored.setOrderIndex(documents.size());
        stored.setSignedPlayback(true);
        documents.add(stored);
        content.setDocumentsDraftJson(writeJson(documents));
        content.setUpdatedBy(userId);
        lessonContentRepository.persist(content);
        return toClientDocument(stored);
    }

    @Transactional
    public DocumentViewDto getDocumentViewUrl(
            Long tenantId,
            String categoryUuid,
            String documentUuid,
            Long userId,
            boolean canManage
    ) {
        CourseCategory category = findCategory(tenantId, categoryUuid);
        String objectKey = resolveDocumentObjectKey(tenantId, category, documentUuid, canManage);
        if (!canManage) {
            requirePaidAccess(tenantId, userId, category, false);
        }

        R2StorageService.PresignedPlayback signed = r2StorageService.presignGet(objectKey);
        DocumentViewDto dto = new DocumentViewDto();
        dto.setDocumentId(documentUuid);
        dto.setUrl(signed.url());
        dto.setExpiresAt(signed.expiresAt());
        dto.setProvider(PROVIDER_R2);
        dto.setFormat(detectDocumentFormat(objectKey));
        return dto;
    }

    @Transactional
    public VideoPlaybackDto getPlaybackUrl(
            Long tenantId,
            String categoryUuid,
            String videoUuid,
            Long userId,
            boolean canManage
    ) {
        CourseCategory category = findCategory(tenantId, categoryUuid);
        String objectKey = resolveVideoObjectKey(tenantId, category, videoUuid, canManage);
        if (!canManage) {
            requirePaidAccess(tenantId, userId, category, false);
        }

        R2StorageService.PresignedPlayback signed = r2StorageService.presignGet(objectKey);
        VideoPlaybackDto dto = new VideoPlaybackDto();
        dto.setVideoId(videoUuid);
        dto.setUrl(signed.url());
        dto.setExpiresAt(signed.expiresAt());
        dto.setProvider(PROVIDER_R2);
        return dto;
    }

    public boolean isR2Enabled() {
        return r2StorageService.isEnabled();
    }

    private void requirePaidAccess(Long tenantId, Long userId, CourseCategory category, boolean canManage) {
        if (!courseSubscriptionService.canViewContent(tenantId, userId, category, canManage)) {
            throw new BusinessException(402, "Subscribe to this programme, year, semester, or course unit to open the outline.");
        }
    }

    private void requireEnrollment(Long tenantId, Long userId, CourseCategory category) {
        CourseCategory root = category;
        int guard = 0;
        while (root.getParent() != null && guard++ < 50) {
            root = root.getParent();
        }

        CourseEnrollment enrollment = enrollmentRepository
                .findByUserAndCategory(userId, root.getId())
                .orElse(null);
        if (enrollment == null || !"ACTIVE".equals(enrollment.getEnrollmentStatus())) {
            throw new ForbiddenException("Enroll in this course to access protected lesson media.");
        }
    }

    private CourseLessonDocument findDocument(Long tenantId, String documentUuidText) {
        UUID uuid;
        try {
            uuid = UUID.fromString(documentUuidText);
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Document not found: " + documentUuidText);
        }
        return lessonDocumentRepository.findByTenantAndUuid(tenantId, uuid)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentUuidText));
    }

    private CourseLessonSlide findSlide(Long tenantId, String slideUuidText) {
        UUID uuid;
        try {
            uuid = UUID.fromString(slideUuidText);
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Slide not found: " + slideUuidText);
        }
        return lessonSlideRepository.findByTenantAndUuid(tenantId, uuid)
                .orElseThrow(() -> new NotFoundException("Slide not found: " + slideUuidText));
    }

    private CourseLessonVideo findVideo(Long tenantId, String videoUuidText) {
        UUID uuid;
        try {
            uuid = UUID.fromString(videoUuidText);
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Video not found: " + videoUuidText);
        }
        return lessonVideoRepository.findByTenantAndUuid(tenantId, uuid)
                .orElseThrow(() -> new NotFoundException("Video not found: " + videoUuidText));
    }

    private CourseCategory findCategory(Long tenantId, String categoryUuid) {
        UUID uuid;
        try {
            uuid = UUID.fromString(categoryUuid);
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Course category not found: " + categoryUuid);
        }

        CourseCategory category = courseCategoryRepository
                .findByTenantAndUuid(tenantId, uuid)
                .orElseThrow(() -> new NotFoundException("Course category not found: " + categoryUuid));

        if (!"ACTIVE".equals(category.getStatus())) {
            throw new NotFoundException("Course category not found: " + categoryUuid);
        }
        return category;
    }

    private LessonDocumentDto toDocumentDto(CourseLessonDocument document) {
        LessonDocumentDto dto = new LessonDocumentDto();
        dto.setId(document.getUuid().toString());
        dto.setTitle(document.getTitle());
        boolean r2 = PROVIDER_R2.equalsIgnoreCase(document.getProvider());
        dto.setProvider(document.getProvider());
        dto.setSignedPlayback(r2);
        dto.setUrl(r2 ? "" : document.getUrl());
        dto.setFileFormat(document.getFileFormat());
        dto.setOrderIndex(document.getOrderIndex());
        return dto;
    }

    private LessonLiveSessionDto toLiveSessionDto(CourseLessonLiveSession session) {
        LessonLiveSessionDto dto = new LessonLiveSessionDto();
        dto.setId(session.getUuid().toString());
        dto.setTitle(session.getTitle());
        dto.setUrl(session.getUrl());
        dto.setProvider(session.getProvider());
        dto.setScheduledAt(session.getScheduledAt() != null ? session.getScheduledAt().toString() : null);
        dto.setDurationMinutes(session.getDurationMinutes());
        dto.setNotes(session.getNotes());
        dto.setOrderIndex(session.getOrderIndex());
        return dto;
    }

    private static String detectDocumentFormat(String objectKeyOrUrl) {
        if (objectKeyOrUrl == null) {
            return "other";
        }
        String lower = objectKeyOrUrl.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "word";
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return "excel";
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt") || lower.endsWith(".ppsx")) return "pptx";
        if (lower.endsWith(".txt")) return "txt";
        if (lower.endsWith(".csv")) return "csv";
        if (lower.endsWith(".rtf")) return "rtf";
        if (lower.endsWith(".odt")) return "odt";
        if (lower.endsWith(".odp")) return "odp";
        return "other";
    }

    private LessonSlideDto toSlideDto(CourseLessonSlide slide) {
        LessonSlideDto dto = new LessonSlideDto();
        dto.setId(slide.getUuid().toString());
        dto.setTitle(slide.getTitle());
        boolean r2 = PROVIDER_R2.equalsIgnoreCase(slide.getProvider());
        dto.setProvider(slide.getProvider());
        dto.setSignedPlayback(r2);
        dto.setUrl(r2 ? "" : slide.getUrl());
        dto.setOrderIndex(slide.getOrderIndex());
        return dto;
    }

    private static String detectSlideFormat(String objectKey) {
        if (objectKey == null) {
            return "other";
        }
        String lower = objectKey.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "pdf";
        }
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt") || lower.endsWith(".ppsx")) {
            return "pptx";
        }
        return "other";
    }

    private LessonVideoDto toVideoDto(CourseLessonVideo video) {
        LessonVideoDto dto = new LessonVideoDto();
        dto.setId(video.getUuid().toString());
        dto.setTitle(video.getTitle());
        boolean r2 = PROVIDER_R2.equalsIgnoreCase(video.getProvider());
        dto.setProvider(video.getProvider());
        dto.setSignedPlayback(r2);
        // Never expose the private object key as a playable URL.
        dto.setUrl(r2 ? "" : video.getUrl());
        dto.setOrderIndex(video.getOrderIndex());
        return dto;
    }

    private static String safeTitle(String title, String fallback) {
        if (title == null || title.isBlank()) {
            return fallback;
        }
        return title.trim();
    }

    private static void applyNotesForReader(CourseLessonDto dto, CourseLessonContent content, boolean forEditor) {
        String published = content.getNotesBody();
        String draft = content.getNotesDraftBody();
        String editorBody = draft != null ? draft : published;
        boolean unpublished = !blankToEmpty(editorBody).equals(blankToEmpty(published));
        dto.setNotesBody(forEditor ? editorBody : published);
        dto.setHasUnpublishedNotes(forEditor && unpublished);
    }

    private CourseLessonContent ensureLessonContent(CourseCategory category, Long tenantId, Long userId) {
        return lessonContentRepository.findByCategoryId(category.getId()).orElseGet(() -> {
            CourseLessonContent created = new CourseLessonContent();
            created.setCategory(category);
            created.setStatus("ACTIVE");
            created.setCreatedBy(userId);
            Tenant tenant = new Tenant();
            tenant.setId(tenantId);
            created.setTenant(tenant);
            lessonContentRepository.persist(created);
            return created;
        });
    }

    private static boolean hasMediaDraft(CourseLessonContent content) {
        return content.getSlidesDraftJson() != null
                || content.getVideosDraftJson() != null
                || content.getDocumentsDraftJson() != null
                || content.getLiveSessionsDraftJson() != null;
    }

    private void seedMediaDraftFromPublished(CourseLessonContent content, Long categoryId) {
        content.setSlidesDraftJson(writeJson(publishedSlidesStored(categoryId)));
        content.setVideosDraftJson(writeJson(publishedVideosStored(categoryId)));
        content.setDocumentsDraftJson(writeJson(publishedDocumentsStored(categoryId)));
        content.setLiveSessionsDraftJson(writeJson(publishedLiveSessionsStored(categoryId)));
    }

    private void storeMediaDrafts(CourseLessonContent content, CourseCategory category, SaveCourseLessonRequest request) {
        List<LessonSlideDto> previousSlides = hasMediaDraft(content)
                ? readDraftSlides(content)
                : publishedSlidesStored(category.getId());
        List<LessonVideoDto> previousVideos = hasMediaDraft(content)
                ? readDraftVideos(content)
                : publishedVideosStored(category.getId());
        List<LessonDocumentDto> previousDocuments = hasMediaDraft(content)
                ? readDraftDocuments(content)
                : publishedDocumentsStored(category.getId());
        List<LessonLiveSessionDto> previousLiveSessions = hasMediaDraft(content)
                ? readDraftLiveSessions(content)
                : publishedLiveSessionsStored(category.getId());

        List<LessonSlideDto> nextSlides = mergeSlideDrafts(
                request.getSlides(), previousSlides, publishedSlidesStored(category.getId()));
        List<LessonVideoDto> nextVideos = mergeVideoDrafts(
                request.getVideos(), previousVideos, publishedVideosStored(category.getId()));
        List<LessonDocumentDto> nextDocuments = mergeDocumentDrafts(
                request.getDocuments(), previousDocuments, publishedDocumentsStored(category.getId()));
        List<LessonLiveSessionDto> nextLiveSessions = mergeLiveSessionDrafts(
                request.getLiveSessions(), previousLiveSessions, publishedLiveSessionsStored(category.getId()));

        Set<String> publishedKeys = publishedR2Keys(category.getId());
        Set<String> nextKeys = new HashSet<>();
        nextKeys.addAll(r2KeysFromSlides(nextSlides));
        nextKeys.addAll(r2KeysFromVideos(nextVideos));
        nextKeys.addAll(r2KeysFromDocuments(nextDocuments));
        Set<String> previousKeys = new HashSet<>();
        previousKeys.addAll(r2KeysFromSlides(previousSlides));
        previousKeys.addAll(r2KeysFromVideos(previousVideos));
        previousKeys.addAll(r2KeysFromDocuments(previousDocuments));
        for (String key : previousKeys) {
            if (!nextKeys.contains(key) && !publishedKeys.contains(key)) {
                r2StorageService.deleteObject(key);
            }
        }

        content.setSlidesDraftJson(writeJson(nextSlides));
        content.setVideosDraftJson(writeJson(nextVideos));
        content.setDocumentsDraftJson(writeJson(nextDocuments));
        content.setLiveSessionsDraftJson(writeJson(nextLiveSessions));
    }

    private boolean hasUnpublishedMedia(CourseLessonContent content, Long categoryId) {
        if (!hasMediaDraft(content)) {
            return false;
        }
        return !sameSlideDrafts(readDraftSlides(content), publishedSlidesStored(categoryId))
                || !sameVideoDrafts(readDraftVideos(content), publishedVideosStored(categoryId))
                || !sameDocumentDrafts(readDraftDocuments(content), publishedDocumentsStored(categoryId))
                || !sameLiveSessionDrafts(readDraftLiveSessions(content), publishedLiveSessionsStored(categoryId));
    }

    private void replacePublishedMedia(
            Long tenantId,
            CourseCategory category,
            Long userId,
            List<LessonSlideDto> slides,
            List<LessonVideoDto> videos,
            List<LessonDocumentDto> documents,
            List<LessonLiveSessionDto> liveSessions
    ) {
        Set<String> keptObjectKeys = new HashSet<>();
        keptObjectKeys.addAll(r2KeysFromSlides(slides));
        keptObjectKeys.addAll(r2KeysFromVideos(videos));
        keptObjectKeys.addAll(r2KeysFromDocuments(documents));

        for (CourseLessonSlide existing : lessonSlideRepository.findByCategoryId(category.getId())) {
            if (PROVIDER_R2.equalsIgnoreCase(existing.getProvider())
                    && existing.getUrl() != null
                    && !keptObjectKeys.contains(existing.getUrl())) {
                r2StorageService.deleteObject(existing.getUrl());
            }
        }
        for (CourseLessonVideo existing : lessonVideoRepository.findByCategoryId(category.getId())) {
            if (PROVIDER_R2.equalsIgnoreCase(existing.getProvider())
                    && existing.getUrl() != null
                    && !keptObjectKeys.contains(existing.getUrl())) {
                r2StorageService.deleteObject(existing.getUrl());
            }
        }
        for (CourseLessonDocument existing : lessonDocumentRepository.findByCategoryId(category.getId())) {
            if (PROVIDER_R2.equalsIgnoreCase(existing.getProvider())
                    && existing.getUrl() != null
                    && !keptObjectKeys.contains(existing.getUrl())) {
                r2StorageService.deleteObject(existing.getUrl());
            }
        }

        lessonSlideRepository.deleteByCategoryId(category.getId());
        lessonVideoRepository.deleteByCategoryId(category.getId());
        lessonDocumentRepository.deleteByCategoryId(category.getId());
        lessonLiveSessionRepository.deleteByCategoryId(category.getId());

        for (int i = 0; i < slides.size(); i++) {
            LessonSlideDto slideDto = slides.get(i);
            if (slideDto.getUrl() == null || slideDto.getUrl().isBlank()) {
                continue;
            }
            CourseLessonSlide slide = new CourseLessonSlide();
            slide.setCategory(category);
            slide.setTitle(safeTitle(slideDto.getTitle(), "Presentation " + (i + 1)));
            slide.setUrl(slideDto.getUrl().trim());
            slide.setProvider(blankToEmpty(slideDto.getProvider()).isEmpty()
                    ? "google-slides"
                    : slideDto.getProvider());
            slide.setOrderIndex(slideDto.getOrderIndex() != null ? slideDto.getOrderIndex() : i);
            slide.setStatus("ACTIVE");
            slide.setCreatedBy(userId);
            slide.setUpdatedBy(userId);
            assignUuid(slideDto.getId(), slide::setUuid);
            Tenant tenant = new Tenant();
            tenant.setId(tenantId);
            slide.setTenant(tenant);
            lessonSlideRepository.persist(slide);
        }

        for (int i = 0; i < videos.size(); i++) {
            LessonVideoDto videoDto = videos.get(i);
            if (videoDto.getUrl() == null || videoDto.getUrl().isBlank()) {
                continue;
            }
            CourseLessonVideo video = new CourseLessonVideo();
            video.setCategory(category);
            video.setTitle(safeTitle(videoDto.getTitle(), "Video " + (i + 1)));
            video.setUrl(videoDto.getUrl().trim());
            video.setProvider(blankToEmpty(videoDto.getProvider()).isEmpty()
                    ? detectVideoProvider(videoDto.getUrl())
                    : videoDto.getProvider());
            video.setOrderIndex(videoDto.getOrderIndex() != null ? videoDto.getOrderIndex() : i);
            video.setStatus("ACTIVE");
            video.setCreatedBy(userId);
            video.setUpdatedBy(userId);
            assignUuid(videoDto.getId(), video::setUuid);
            Tenant tenant = new Tenant();
            tenant.setId(tenantId);
            video.setTenant(tenant);
            lessonVideoRepository.persist(video);
        }

        for (int i = 0; i < documents.size(); i++) {
            LessonDocumentDto documentDto = documents.get(i);
            if (documentDto.getUrl() == null || documentDto.getUrl().isBlank()) {
                continue;
            }
            CourseLessonDocument document = new CourseLessonDocument();
            document.setCategory(category);
            document.setTitle(safeTitle(documentDto.getTitle(), "Document " + (i + 1)));
            document.setUrl(documentDto.getUrl().trim());
            document.setProvider(blankToEmpty(documentDto.getProvider()).isEmpty() ? "link" : documentDto.getProvider());
            document.setFileFormat(
                    documentDto.getFileFormat() != null && !documentDto.getFileFormat().isBlank()
                            ? documentDto.getFileFormat()
                            : detectDocumentFormat(documentDto.getUrl())
            );
            document.setOrderIndex(documentDto.getOrderIndex() != null ? documentDto.getOrderIndex() : i);
            document.setStatus("ACTIVE");
            document.setCreatedBy(userId);
            document.setUpdatedBy(userId);
            assignUuid(documentDto.getId(), document::setUuid);
            Tenant tenant = new Tenant();
            tenant.setId(tenantId);
            document.setTenant(tenant);
            lessonDocumentRepository.persist(document);
        }

        for (int i = 0; i < liveSessions.size(); i++) {
            LessonLiveSessionDto sessionDto = liveSessions.get(i);
            if (sessionDto.getUrl() == null || sessionDto.getUrl().isBlank()) {
                continue;
            }
            CourseLessonLiveSession session = new CourseLessonLiveSession();
            session.setCategory(category);
            session.setTitle(safeTitle(sessionDto.getTitle(), "Live session " + (i + 1)));
            session.setUrl(sessionDto.getUrl().trim());
            session.setProvider(blankToEmpty(sessionDto.getProvider()).isEmpty()
                    ? detectLiveProvider(sessionDto.getUrl())
                    : sessionDto.getProvider());
            session.setScheduledAt(parseScheduledAt(sessionDto.getScheduledAt()));
            session.setDurationMinutes(sessionDto.getDurationMinutes());
            session.setNotes(blankToNull(sessionDto.getNotes()));
            session.setOrderIndex(sessionDto.getOrderIndex() != null ? sessionDto.getOrderIndex() : i);
            session.setStatus("ACTIVE");
            session.setCreatedBy(userId);
            session.setUpdatedBy(userId);
            assignUuid(sessionDto.getId(), session::setUuid);
            Tenant tenant = new Tenant();
            tenant.setId(tenantId);
            session.setTenant(tenant);
            lessonLiveSessionRepository.persist(session);
        }
    }

    private List<LessonSlideDto> mergeSlideDrafts(
            List<LessonSlideDto> incoming,
            List<LessonSlideDto> previous,
            List<LessonSlideDto> published
    ) {
        Map<String, LessonSlideDto> previousById = indexSlides(previous);
        Map<String, LessonSlideDto> publishedById = indexSlides(published);
        List<LessonSlideDto> source = incoming != null ? incoming : List.of();
        List<LessonSlideDto> out = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            LessonSlideDto item = source.get(i);
            LessonSlideDto row = new LessonSlideDto();
            String id = item.getId() != null && !item.getId().isBlank() ? item.getId() : UUID.randomUUID().toString();
            row.setId(id);
            row.setTitle(safeTitle(item.getTitle(), "Presentation " + (i + 1)));
            row.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
            boolean r2 = PROVIDER_R2.equalsIgnoreCase(blankToEmpty(item.getProvider()));
            if (r2) {
                String key = firstObjectKey(
                        item.getUrl(),
                        previousById.get(id) != null ? previousById.get(id).getUrl() : null,
                        publishedById.get(id) != null ? publishedById.get(id).getUrl() : null
                );
                if (key == null) {
                    continue;
                }
                row.setProvider(PROVIDER_R2);
                row.setUrl(key);
                row.setSignedPlayback(true);
            } else {
                if (item.getUrl() == null || item.getUrl().isBlank()) {
                    continue;
                }
                row.setProvider(item.getProvider() != null ? item.getProvider() : "google-slides");
                row.setUrl(item.getUrl().trim());
            }
            out.add(row);
        }
        return out;
    }

    private List<LessonVideoDto> mergeVideoDrafts(
            List<LessonVideoDto> incoming,
            List<LessonVideoDto> previous,
            List<LessonVideoDto> published
    ) {
        Map<String, LessonVideoDto> previousById = indexVideos(previous);
        Map<String, LessonVideoDto> publishedById = indexVideos(published);
        List<LessonVideoDto> source = incoming != null ? incoming : List.of();
        List<LessonVideoDto> out = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            LessonVideoDto item = source.get(i);
            LessonVideoDto row = new LessonVideoDto();
            String id = item.getId() != null && !item.getId().isBlank() ? item.getId() : UUID.randomUUID().toString();
            row.setId(id);
            row.setTitle(safeTitle(item.getTitle(), "Video " + (i + 1)));
            row.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
            boolean r2 = PROVIDER_R2.equalsIgnoreCase(blankToEmpty(item.getProvider()));
            if (r2) {
                String key = firstObjectKey(
                        item.getUrl(),
                        previousById.get(id) != null ? previousById.get(id).getUrl() : null,
                        publishedById.get(id) != null ? publishedById.get(id).getUrl() : null
                );
                if (key == null) {
                    continue;
                }
                row.setProvider(PROVIDER_R2);
                row.setUrl(key);
                row.setSignedPlayback(true);
            } else {
                if (item.getUrl() == null || item.getUrl().isBlank()) {
                    continue;
                }
                String url = item.getUrl().trim();
                row.setProvider(item.getProvider() != null ? item.getProvider() : detectVideoProvider(url));
                row.setUrl(url);
            }
            out.add(row);
        }
        return out;
    }

    private List<LessonDocumentDto> mergeDocumentDrafts(
            List<LessonDocumentDto> incoming,
            List<LessonDocumentDto> previous,
            List<LessonDocumentDto> published
    ) {
        Map<String, LessonDocumentDto> previousById = indexDocuments(previous);
        Map<String, LessonDocumentDto> publishedById = indexDocuments(published);
        List<LessonDocumentDto> source = incoming != null ? incoming : List.of();
        List<LessonDocumentDto> out = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            LessonDocumentDto item = source.get(i);
            LessonDocumentDto row = new LessonDocumentDto();
            String id = item.getId() != null && !item.getId().isBlank() ? item.getId() : UUID.randomUUID().toString();
            row.setId(id);
            row.setTitle(safeTitle(item.getTitle(), "Document " + (i + 1)));
            row.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
            boolean r2 = PROVIDER_R2.equalsIgnoreCase(blankToEmpty(item.getProvider()));
            if (r2) {
                String key = firstObjectKey(
                        item.getUrl(),
                        previousById.get(id) != null ? previousById.get(id).getUrl() : null,
                        publishedById.get(id) != null ? publishedById.get(id).getUrl() : null
                );
                if (key == null) {
                    continue;
                }
                row.setProvider(PROVIDER_R2);
                row.setUrl(key);
                row.setSignedPlayback(true);
                row.setFileFormat(item.getFileFormat() != null ? item.getFileFormat() : detectDocumentFormat(key));
            } else {
                if (item.getUrl() == null || item.getUrl().isBlank()) {
                    continue;
                }
                String url = item.getUrl().trim();
                row.setProvider(item.getProvider() != null ? item.getProvider() : "link");
                row.setUrl(url);
                row.setFileFormat(item.getFileFormat() != null ? item.getFileFormat() : detectDocumentFormat(url));
            }
            out.add(row);
        }
        return out;
    }

    private List<LessonLiveSessionDto> mergeLiveSessionDrafts(
            List<LessonLiveSessionDto> incoming,
            List<LessonLiveSessionDto> previous,
            List<LessonLiveSessionDto> published
    ) {
        Map<String, LessonLiveSessionDto> previousById = indexLiveSessions(previous);
        Map<String, LessonLiveSessionDto> publishedById = indexLiveSessions(published);
        List<LessonLiveSessionDto> source = incoming != null ? incoming : List.of();
        List<LessonLiveSessionDto> out = new ArrayList<>();
        for (int i = 0; i < source.size(); i++) {
            LessonLiveSessionDto item = source.get(i);
            if (item.getUrl() == null || item.getUrl().isBlank()) {
                continue;
            }
            LessonLiveSessionDto row = new LessonLiveSessionDto();
            String id = item.getId() != null && !item.getId().isBlank() ? item.getId() : UUID.randomUUID().toString();
            row.setId(id);
            row.setTitle(safeTitle(item.getTitle(), "Live session " + (i + 1)));
            row.setOrderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : i);
            String url = item.getUrl().trim();
            row.setUrl(url);
            row.setProvider(blankToEmpty(item.getProvider()).isEmpty()
                    ? detectLiveProvider(url)
                    : item.getProvider());
            row.setScheduledAt(blankToNull(item.getScheduledAt()));
            row.setDurationMinutes(item.getDurationMinutes());
            row.setNotes(blankToNull(item.getNotes()));
            if (row.getScheduledAt() == null && previousById.get(id) != null) {
                row.setScheduledAt(previousById.get(id).getScheduledAt());
            }
            if (row.getScheduledAt() == null && publishedById.get(id) != null) {
                row.setScheduledAt(publishedById.get(id).getScheduledAt());
            }
            out.add(row);
        }
        return out;
    }

    private List<LessonSlideDto> publishedSlidesStored(Long categoryId) {
        List<LessonSlideDto> out = new ArrayList<>();
        for (CourseLessonSlide slide : lessonSlideRepository.findByCategoryId(categoryId)) {
            LessonSlideDto dto = new LessonSlideDto();
            dto.setId(slide.getUuid().toString());
            dto.setTitle(slide.getTitle());
            dto.setProvider(slide.getProvider());
            dto.setUrl(slide.getUrl());
            dto.setOrderIndex(slide.getOrderIndex());
            dto.setSignedPlayback(PROVIDER_R2.equalsIgnoreCase(slide.getProvider()));
            out.add(dto);
        }
        return out;
    }

    private List<LessonVideoDto> publishedVideosStored(Long categoryId) {
        List<LessonVideoDto> out = new ArrayList<>();
        for (CourseLessonVideo video : lessonVideoRepository.findByCategoryId(categoryId)) {
            LessonVideoDto dto = new LessonVideoDto();
            dto.setId(video.getUuid().toString());
            dto.setTitle(video.getTitle());
            dto.setProvider(video.getProvider());
            dto.setUrl(video.getUrl());
            dto.setOrderIndex(video.getOrderIndex());
            dto.setSignedPlayback(PROVIDER_R2.equalsIgnoreCase(video.getProvider()));
            out.add(dto);
        }
        return out;
    }

    private List<LessonDocumentDto> publishedDocumentsStored(Long categoryId) {
        List<LessonDocumentDto> out = new ArrayList<>();
        for (CourseLessonDocument document : lessonDocumentRepository.findByCategoryId(categoryId)) {
            LessonDocumentDto dto = new LessonDocumentDto();
            dto.setId(document.getUuid().toString());
            dto.setTitle(document.getTitle());
            dto.setProvider(document.getProvider());
            dto.setUrl(document.getUrl());
            dto.setFileFormat(document.getFileFormat());
            dto.setOrderIndex(document.getOrderIndex());
            dto.setSignedPlayback(PROVIDER_R2.equalsIgnoreCase(document.getProvider()));
            out.add(dto);
        }
        return out;
    }

    private List<LessonLiveSessionDto> publishedLiveSessionsStored(Long categoryId) {
        List<LessonLiveSessionDto> out = new ArrayList<>();
        for (CourseLessonLiveSession session : lessonLiveSessionRepository.findByCategoryId(categoryId)) {
            out.add(toLiveSessionDto(session));
        }
        return out;
    }

    private List<LessonSlideDto> readDraftSlides(CourseLessonContent content) {
        return readJson(content.getSlidesDraftJson(), new TypeReference<List<LessonSlideDto>>() {}, List.of());
    }

    private List<LessonVideoDto> readDraftVideos(CourseLessonContent content) {
        return readJson(content.getVideosDraftJson(), new TypeReference<List<LessonVideoDto>>() {}, List.of());
    }

    private List<LessonDocumentDto> readDraftDocuments(CourseLessonContent content) {
        return readJson(content.getDocumentsDraftJson(), new TypeReference<List<LessonDocumentDto>>() {}, List.of());
    }

    private List<LessonLiveSessionDto> readDraftLiveSessions(CourseLessonContent content) {
        return readJson(content.getLiveSessionsDraftJson(), new TypeReference<List<LessonLiveSessionDto>>() {}, List.of());
    }

    private List<LessonSlideDto> toClientSlides(List<LessonSlideDto> stored) {
        List<LessonSlideDto> out = new ArrayList<>();
        for (LessonSlideDto item : stored) {
            out.add(toClientSlide(item));
        }
        return out;
    }

    private List<LessonVideoDto> toClientVideos(List<LessonVideoDto> stored) {
        List<LessonVideoDto> out = new ArrayList<>();
        for (LessonVideoDto item : stored) {
            out.add(toClientVideo(item));
        }
        return out;
    }

    private List<LessonDocumentDto> toClientDocuments(List<LessonDocumentDto> stored) {
        List<LessonDocumentDto> out = new ArrayList<>();
        for (LessonDocumentDto item : stored) {
            out.add(toClientDocument(item));
        }
        return out;
    }

    private LessonSlideDto toClientSlide(LessonSlideDto stored) {
        LessonSlideDto dto = new LessonSlideDto();
        dto.setId(stored.getId());
        dto.setTitle(stored.getTitle());
        dto.setProvider(stored.getProvider());
        dto.setOrderIndex(stored.getOrderIndex());
        boolean r2 = PROVIDER_R2.equalsIgnoreCase(blankToEmpty(stored.getProvider()));
        dto.setSignedPlayback(r2);
        dto.setUrl(r2 ? "" : stored.getUrl());
        return dto;
    }

    private LessonVideoDto toClientVideo(LessonVideoDto stored) {
        LessonVideoDto dto = new LessonVideoDto();
        dto.setId(stored.getId());
        dto.setTitle(stored.getTitle());
        dto.setProvider(stored.getProvider());
        dto.setOrderIndex(stored.getOrderIndex());
        boolean r2 = PROVIDER_R2.equalsIgnoreCase(blankToEmpty(stored.getProvider()));
        dto.setSignedPlayback(r2);
        dto.setUrl(r2 ? "" : stored.getUrl());
        return dto;
    }

    private LessonDocumentDto toClientDocument(LessonDocumentDto stored) {
        LessonDocumentDto dto = new LessonDocumentDto();
        dto.setId(stored.getId());
        dto.setTitle(stored.getTitle());
        dto.setProvider(stored.getProvider());
        dto.setFileFormat(stored.getFileFormat());
        dto.setOrderIndex(stored.getOrderIndex());
        boolean r2 = PROVIDER_R2.equalsIgnoreCase(blankToEmpty(stored.getProvider()));
        dto.setSignedPlayback(r2);
        dto.setUrl(r2 ? "" : stored.getUrl());
        return dto;
    }

    private String resolveSlideObjectKey(Long tenantId, CourseCategory category, String slideUuid, boolean canManage) {
        try {
            CourseLessonSlide slide = findSlide(tenantId, slideUuid);
            if (slide.getCategory().getId().equals(category.getId())
                    && PROVIDER_R2.equalsIgnoreCase(slide.getProvider())) {
                return slide.getUrl();
            }
        } catch (NotFoundException ignored) {
            if (!canManage) {
                throw ignored;
            }
        }
        if (!canManage) {
            throw new NotFoundException("Slide not found: " + slideUuid);
        }
        CourseLessonContent content = lessonContentRepository.findByCategoryId(category.getId()).orElse(null);
        if (content == null) {
            throw new NotFoundException("Slide not found: " + slideUuid);
        }
        for (LessonSlideDto draft : readDraftSlides(content)) {
            if (slideUuid.equals(draft.getId()) && PROVIDER_R2.equalsIgnoreCase(blankToEmpty(draft.getProvider()))) {
                return draft.getUrl();
            }
        }
        throw new NotFoundException("Slide not found: " + slideUuid);
    }

    private String resolveVideoObjectKey(Long tenantId, CourseCategory category, String videoUuid, boolean canManage) {
        try {
            CourseLessonVideo video = findVideo(tenantId, videoUuid);
            if (video.getCategory().getId().equals(category.getId())
                    && PROVIDER_R2.equalsIgnoreCase(video.getProvider())) {
                return video.getUrl();
            }
        } catch (NotFoundException ignored) {
            if (!canManage) {
                throw ignored;
            }
        }
        if (!canManage) {
            throw new NotFoundException("Video not found: " + videoUuid);
        }
        CourseLessonContent content = lessonContentRepository.findByCategoryId(category.getId()).orElse(null);
        if (content == null) {
            throw new NotFoundException("Video not found: " + videoUuid);
        }
        for (LessonVideoDto draft : readDraftVideos(content)) {
            if (videoUuid.equals(draft.getId()) && PROVIDER_R2.equalsIgnoreCase(blankToEmpty(draft.getProvider()))) {
                return draft.getUrl();
            }
        }
        throw new NotFoundException("Video not found: " + videoUuid);
    }

    private String resolveDocumentObjectKey(Long tenantId, CourseCategory category, String documentUuid, boolean canManage) {
        try {
            CourseLessonDocument document = findDocument(tenantId, documentUuid);
            if (document.getCategory().getId().equals(category.getId())
                    && PROVIDER_R2.equalsIgnoreCase(document.getProvider())) {
                return document.getUrl();
            }
        } catch (NotFoundException ignored) {
            if (!canManage) {
                throw ignored;
            }
        }
        if (!canManage) {
            throw new NotFoundException("Document not found: " + documentUuid);
        }
        CourseLessonContent content = lessonContentRepository.findByCategoryId(category.getId()).orElse(null);
        if (content == null) {
            throw new NotFoundException("Document not found: " + documentUuid);
        }
        for (LessonDocumentDto draft : readDraftDocuments(content)) {
            if (documentUuid.equals(draft.getId()) && PROVIDER_R2.equalsIgnoreCase(blankToEmpty(draft.getProvider()))) {
                return draft.getUrl();
            }
        }
        throw new NotFoundException("Document not found: " + documentUuid);
    }

    private Set<String> publishedR2Keys(Long categoryId) {
        Set<String> keys = new HashSet<>();
        keys.addAll(r2KeysFromSlides(publishedSlidesStored(categoryId)));
        keys.addAll(r2KeysFromVideos(publishedVideosStored(categoryId)));
        keys.addAll(r2KeysFromDocuments(publishedDocumentsStored(categoryId)));
        return keys;
    }

    private static Set<String> r2KeysFromSlides(List<LessonSlideDto> items) {
        Set<String> keys = new HashSet<>();
        for (LessonSlideDto item : items) {
            if (PROVIDER_R2.equalsIgnoreCase(blankToEmpty(item.getProvider())) && item.getUrl() != null && !item.getUrl().isBlank()) {
                keys.add(item.getUrl());
            }
        }
        return keys;
    }

    private static Set<String> r2KeysFromVideos(List<LessonVideoDto> items) {
        Set<String> keys = new HashSet<>();
        for (LessonVideoDto item : items) {
            if (PROVIDER_R2.equalsIgnoreCase(blankToEmpty(item.getProvider())) && item.getUrl() != null && !item.getUrl().isBlank()) {
                keys.add(item.getUrl());
            }
        }
        return keys;
    }

    private static Set<String> r2KeysFromDocuments(List<LessonDocumentDto> items) {
        Set<String> keys = new HashSet<>();
        for (LessonDocumentDto item : items) {
            if (PROVIDER_R2.equalsIgnoreCase(blankToEmpty(item.getProvider())) && item.getUrl() != null && !item.getUrl().isBlank()) {
                keys.add(item.getUrl());
            }
        }
        return keys;
    }

    private static boolean sameSlideDrafts(List<LessonSlideDto> a, List<LessonSlideDto> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            LessonSlideDto left = a.get(i);
            LessonSlideDto right = b.get(i);
            if (!Objects.equals(blankToEmpty(left.getId()), blankToEmpty(right.getId()))
                    || !Objects.equals(blankToEmpty(left.getTitle()), blankToEmpty(right.getTitle()))
                    || !Objects.equals(blankToEmpty(left.getProvider()), blankToEmpty(right.getProvider()))
                    || !Objects.equals(blankToEmpty(left.getUrl()), blankToEmpty(right.getUrl()))
                    || !Objects.equals(left.getOrderIndex(), right.getOrderIndex())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameVideoDrafts(List<LessonVideoDto> a, List<LessonVideoDto> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            LessonVideoDto left = a.get(i);
            LessonVideoDto right = b.get(i);
            if (!Objects.equals(blankToEmpty(left.getId()), blankToEmpty(right.getId()))
                    || !Objects.equals(blankToEmpty(left.getTitle()), blankToEmpty(right.getTitle()))
                    || !Objects.equals(blankToEmpty(left.getProvider()), blankToEmpty(right.getProvider()))
                    || !Objects.equals(blankToEmpty(left.getUrl()), blankToEmpty(right.getUrl()))
                    || !Objects.equals(left.getOrderIndex(), right.getOrderIndex())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameDocumentDrafts(List<LessonDocumentDto> a, List<LessonDocumentDto> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            LessonDocumentDto left = a.get(i);
            LessonDocumentDto right = b.get(i);
            if (!Objects.equals(blankToEmpty(left.getId()), blankToEmpty(right.getId()))
                    || !Objects.equals(blankToEmpty(left.getTitle()), blankToEmpty(right.getTitle()))
                    || !Objects.equals(blankToEmpty(left.getProvider()), blankToEmpty(right.getProvider()))
                    || !Objects.equals(blankToEmpty(left.getUrl()), blankToEmpty(right.getUrl()))
                    || !Objects.equals(left.getOrderIndex(), right.getOrderIndex())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameLiveSessionDrafts(List<LessonLiveSessionDto> a, List<LessonLiveSessionDto> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            LessonLiveSessionDto left = a.get(i);
            LessonLiveSessionDto right = b.get(i);
            if (!Objects.equals(blankToEmpty(left.getId()), blankToEmpty(right.getId()))
                    || !Objects.equals(blankToEmpty(left.getTitle()), blankToEmpty(right.getTitle()))
                    || !Objects.equals(blankToEmpty(left.getProvider()), blankToEmpty(right.getProvider()))
                    || !Objects.equals(blankToEmpty(left.getUrl()), blankToEmpty(right.getUrl()))
                    || !Objects.equals(blankToNull(left.getScheduledAt()), blankToNull(right.getScheduledAt()))
                    || !Objects.equals(left.getDurationMinutes(), right.getDurationMinutes())
                    || !Objects.equals(blankToNull(left.getNotes()), blankToNull(right.getNotes()))
                    || !Objects.equals(left.getOrderIndex(), right.getOrderIndex())) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, LessonSlideDto> indexSlides(List<LessonSlideDto> items) {
        Map<String, LessonSlideDto> map = new HashMap<>();
        for (LessonSlideDto item : items) {
            if (item.getId() != null) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    private static Map<String, LessonVideoDto> indexVideos(List<LessonVideoDto> items) {
        Map<String, LessonVideoDto> map = new HashMap<>();
        for (LessonVideoDto item : items) {
            if (item.getId() != null) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    private static Map<String, LessonDocumentDto> indexDocuments(List<LessonDocumentDto> items) {
        Map<String, LessonDocumentDto> map = new HashMap<>();
        for (LessonDocumentDto item : items) {
            if (item.getId() != null) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    private static Map<String, LessonLiveSessionDto> indexLiveSessions(List<LessonLiveSessionDto> items) {
        Map<String, LessonLiveSessionDto> map = new HashMap<>();
        for (LessonLiveSessionDto item : items) {
            if (item.getId() != null) {
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    private static String firstObjectKey(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !value.toLowerCase(Locale.ROOT).startsWith("http")) {
                return value.trim();
            }
        }
        return null;
    }

    private static void assignUuid(String id, java.util.function.Consumer<UUID> setter) {
        if (id == null) {
            return;
        }
        try {
            setter.accept(UUID.fromString(id));
        } catch (IllegalArgumentException ignored) {
            // new uuid from BaseEntity PrePersist
        }
    }

    private <T> T readJson(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "Could not read lesson draft.");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "Could not save lesson draft.");
        }
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static OffsetDateTime parseScheduledAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException(400, "Invalid live session schedule time.");
        }
    }

    private static String detectLiveProvider(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("zoom.us")) {
            return "zoom";
        }
        if (lower.contains("meet.google.com")) {
            return "google-meet";
        }
        if (lower.contains("teams.microsoft.com") || lower.contains("teams.live.com")) {
            return "microsoft-teams";
        }
        return "other";
    }

    private static String detectVideoProvider(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return "youtube";
        }
        if (lower.matches(".*\\.(mp4|webm|ogg)(\\?.*)?$")) {
            return "mp4";
        }
        return "other";
    }
}
