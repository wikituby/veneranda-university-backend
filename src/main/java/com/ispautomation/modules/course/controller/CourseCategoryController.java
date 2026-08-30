package com.ispautomation.modules.course.controller;

import com.ispautomation.common.exception.BusinessException;
import com.ispautomation.modules.course.dto.CourseAccessDto;
import com.ispautomation.modules.course.dto.CourseCategoryDto;
import com.ispautomation.modules.course.dto.CoverImageDto;
import com.ispautomation.modules.course.dto.CourseEnrollmentDto;
import com.ispautomation.modules.course.dto.CreatorDashboardDto;
import com.ispautomation.modules.course.dto.CourseLessonDto;
import com.ispautomation.modules.course.dto.CourseSubscriptionDto;
import com.ispautomation.modules.course.dto.CreateCourseCategoryRequest;
import com.ispautomation.modules.course.dto.DocumentViewDto;
import com.ispautomation.modules.course.dto.LessonDocumentDto;
import com.ispautomation.modules.course.dto.LessonSlideDto;
import com.ispautomation.modules.course.dto.LessonVideoDto;
import com.ispautomation.modules.course.dto.SaveCourseLessonRequest;
import com.ispautomation.modules.course.dto.SlideViewDto;
import com.ispautomation.modules.course.dto.SyncCourseGroupMemberRequest;
import com.ispautomation.modules.course.dto.UnenrollRequest;
import com.ispautomation.modules.course.dto.UpdateCourseCategoryRequest;
import com.ispautomation.modules.course.dto.VideoPlaybackDto;
import com.ispautomation.modules.course.service.CourseCategoryService;
import com.ispautomation.modules.course.service.CourseEnrollmentService;
import com.ispautomation.modules.course.service.CourseLessonService;
import com.ispautomation.modules.course.service.CourseSubscriptionService;
import com.ispautomation.modules.course.service.CreatorDashboardService;
import com.ispautomation.security.SecurityContext;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@Path("/api/v1/course-categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Course Categories", description = "Hierarchical course outline categories")
public class CourseCategoryController {

    @Inject
    CourseCategoryService courseCategoryService;

    @Inject
    CourseLessonService courseLessonService;

    @Inject
    CourseEnrollmentService courseEnrollmentService;

    @Inject
    CourseSubscriptionService courseSubscriptionService;

    @Inject
    CreatorDashboardService creatorDashboardService;

    @Inject
    SecurityContext securityContext;

    /** Staff roles allowed to create/edit course outline and lesson media. */
    private static final String[] CONTENT_MANAGER_ROLES = {
            "SUPER_ADMIN",
            "SYSTEM_ADMIN",
            "ADMIN",
            "INSTRUCTOR",
            "TEACHER",
            "LECTURER",
            "COORDINATOR"
    };

    private boolean hasContentManagerRole() {
        for (String role : CONTENT_MANAGER_ROLES) {
            if (securityContext.hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    private void requireCatalogueAccess() {
        securityContext.requireAuthenticated();
    }

    private void requireCourseRead() {
        securityContext.requireAuthenticated();
        if (hasContentManagerRole()
                || securityContext.hasPermission("course:read")
                || securityContext.hasPermission("course:manage")) {
            return;
        }
        securityContext.requirePermission("course:read");
    }

    private void requireCourseManage() {
        securityContext.requireAuthenticated();
        if (hasContentManagerRole() || securityContext.hasPermission("course:manage")) {
            return;
        }
        securityContext.requirePermission("course:manage");
    }

    private void requireCreate(CreateCourseCategoryRequest request) {
        securityContext.requireAuthenticated();
        if (canManageCourse()) {
            return;
        }
        String parentId = request.getParentId();
        if (parentId == null || parentId.isBlank()) {
            return;
        }
        if (courseCategoryService.canEdit(securityContext.getTenantId(), parentId, securityContext.getUserId())) {
            return;
        }
        securityContext.requirePermission("course:manage");
    }

    private void requireEdit(String id) {
        securityContext.requireAuthenticated();
        if (canManageCourse()) {
            return;
        }
        if (courseCategoryService.canEdit(securityContext.getTenantId(), id, securityContext.getUserId())) {
            return;
        }
        securityContext.requirePermission("course:manage");
    }

    @GET
    @Operation(summary = "List course categories", description = "Flat list of categories for the current tenant")
    public Response listCategories(
            @QueryParam("published") @DefaultValue("false") boolean published
    ) {
        requireCatalogueAccess();
        boolean publishedOnly = canManageCourse() ? published : true;
        List<CourseCategoryDto> categories =
                courseCategoryService.listCategories(securityContext.getTenantId(), publishedOnly);
        return Response.ok(categories).build();
    }

    @GET
    @Path("/creator/dashboard")
    @Operation(summary = "Creator analytics for programmes you registered")
    public Response creatorDashboard(@QueryParam("programmeId") String programmeId) {
        requireCatalogueAccess();
        CreatorDashboardDto dashboard = creatorDashboardService.getDashboard(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                programmeId
        );
        return Response.ok(dashboard).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get course category by UUID")
    public Response getCategory(@PathParam("id") String id) {
        requireCatalogueAccess();
        return Response.ok(courseCategoryService.getByUuid(securityContext.getTenantId(), id)).build();
    }

    @POST
    @Operation(summary = "Create course category")
    public Response createCategory(@Valid CreateCourseCategoryRequest request) {
        requireCreate(request);

        CourseCategoryDto created = courseCategoryService.create(
                request,
                securityContext.getTenantId(),
                securityContext.getUserId()
        );
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update course category")
    public Response updateCategory(@PathParam("id") String id, @Valid UpdateCourseCategoryRequest request) {
        requireEdit(id);

        CourseCategoryDto updated = courseCategoryService.update(
                securityContext.getTenantId(),
                id,
                request,
                securityContext.getUserId()
        );
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete course category")
    public Response deleteCategory(@PathParam("id") String id) {
        requireEdit(id);
        courseCategoryService.delete(securityContext.getTenantId(), id);
        return Response.noContent().build();
    }

    @GET
    @Path("/enrollments/mine")
    @Operation(summary = "List my active course enrollments")
    public Response listMyEnrollments() {
        requireCatalogueAccess();
        List<CourseEnrollmentDto> enrollments = courseEnrollmentService.listMine(
                securityContext.getTenantId(),
                securityContext.getUserId()
        );
        return Response.ok(enrollments).build();
    }

    @GET
    @Path("/subscriptions/mine")
    @Operation(summary = "List my semester and course-unit subscriptions")
    public Response listMySubscriptions() {
        requireCatalogueAccess();
        return Response.ok(courseSubscriptionService.listMine(
                securityContext.getTenantId(),
                securityContext.getUserId()
        )).build();
    }

    @GET
    @Path("/{id}/access")
    @Operation(summary = "Enrollment and payment access for this section")
    public Response getAccess(@PathParam("id") String id) {
        requireCatalogueAccess();
        CourseAccessDto access = courseSubscriptionService.getAccess(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id,
                canManageCourse()
        );
        return Response.ok(access).build();
    }

    @POST
    @Path("/{id}/checkout")
    @Operation(summary = "Pay for a semester or course unit (simulated payment)")
    public Response checkout(@PathParam("id") String id, @QueryParam("trial") @DefaultValue("false") boolean trial) {
        requireCatalogueAccess();
        CourseSubscriptionDto paid = courseSubscriptionService.checkout(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id,
                trial
        );
        return Response.ok(paid).build();
    }

    @POST
    @Path("/{id}/unsubscribe")
    @Operation(summary = "Cancel a paid subscription for this programme, year, semester, or unit")
    public Response unsubscribe(@PathParam("id") String id, @Valid UnenrollRequest request) {
        requireCatalogueAccess();
        CourseSubscriptionDto cancelled = courseSubscriptionService.unsubscribe(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id,
                request.getPassword()
        );
        return Response.ok(cancelled).build();
    }

    @GET
    @Path("/{id}/enrollment")
    @Operation(summary = "Get enrollment status for this course (resolved to root)")
    public Response getEnrollment(@PathParam("id") String id) {
        requireCatalogueAccess();
        CourseEnrollmentDto status = courseEnrollmentService.getStatus(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id
        );
        return Response.ok(status).build();
    }

    @POST
    @Path("/{id}/enroll")
    @Operation(summary = "Enroll in course and add Google email to Private video group")
    public Response enroll(@PathParam("id") String id) {
        requireCatalogueAccess();
        CourseEnrollmentDto enrollment = courseEnrollmentService.enroll(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id
        );
        return Response.ok(enrollment).build();
    }



    @GET
    @Path("/{id}/join-requests")
    @Operation(summary = "List pending join requests for a programme (coordinator)")
    public Response listJoinRequests(@PathParam("id") String id) {
        List<CourseEnrollmentDto> pending = courseEnrollmentService.listPendingJoinRequests(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id
        );
        return Response.ok(pending).build();
    }

    @POST
    @Path("/{id}/join-requests/{enrollmentId}/accept")
    @Operation(summary = "Accept a pending join request")
    public Response acceptJoinRequest(@PathParam("id") String id, @PathParam("enrollmentId") String enrollmentId) {
        CourseEnrollmentDto enrollment = courseEnrollmentService.acceptJoinRequest(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id,
                enrollmentId
        );
        return Response.ok(enrollment).build();
    }

    @POST
    @Path("/{id}/join-requests/{enrollmentId}/reject")
    @Operation(summary = "Reject a pending join request")
    public Response rejectJoinRequest(@PathParam("id") String id, @PathParam("enrollmentId") String enrollmentId) {
        CourseEnrollmentDto enrollment = courseEnrollmentService.rejectJoinRequest(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id,
                enrollmentId
        );
        return Response.ok(enrollment).build();
    }

    @POST
    @Path("/{id}/unenroll")
    @Operation(summary = "Unenroll from course after password confirmation; ends subscriptions with no refund")
    public Response unenroll(@PathParam("id") String id, @Valid UnenrollRequest request) {
        requireCatalogueAccess();
        CourseEnrollmentDto enrollment = courseEnrollmentService.unenroll(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                id,
                request.getPassword()
        );
        return Response.ok(enrollment).build();
    }

    @POST
    @Path("/{id}/group-members/add")
    @Operation(summary = "Add a member to this section's Google Group")
    public Response addGroupMember(@PathParam("id") String id, @Valid SyncCourseGroupMemberRequest request) {
        requireCourseManage();
        courseCategoryService.addGroupMember(securityContext.getTenantId(), id, request.getEmail());
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/group-members/remove")
    @Operation(summary = "Remove a member from this section's Google Group")
    public Response removeGroupMember(@PathParam("id") String id, @Valid SyncCourseGroupMemberRequest request) {
        requireCourseManage();
        courseCategoryService.removeGroupMember(securityContext.getTenantId(), id, request.getEmail());
        return Response.noContent().build();
    }

    private boolean canManageCourse() {
        return hasContentManagerRole() || securityContext.hasPermission("course:manage");
    }

    @GET
    @Path("/r2/status")
    @Operation(summary = "Whether Cloudflare R2 video storage is configured")
    public Response r2Status() {
        requireCourseRead();
        return Response.ok(Map.of("enabled", courseCategoryService.isR2Enabled())).build();
    }

    @POST
    @Path("/{id}/cover/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload a programme cover image to Cloudflare R2")
    public Response uploadCover(
            @PathParam("id") String id,
            @RestForm("file") FileUpload file
    ) throws Exception {
        requireEdit(id);
        if (file == null || file.size() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Cover image file is required"))
                    .build();
        }

        try (InputStream data = Files.newInputStream(file.uploadedFile())) {
            CourseCategoryDto uploaded = courseCategoryService.uploadCoverImage(
                    securityContext.getTenantId(),
                    id,
                    securityContext.getUserId(),
                    file.fileName(),
                    file.contentType(),
                    file.size(),
                    data
            );
            return Response.status(Response.Status.CREATED).entity(uploaded).build();
        }
    }

    @GET
    @Path("/{id}/cover")
    @Operation(summary = "Get a short-lived signed URL for a programme cover image")
    public Response getCover(@PathParam("id") String id) {
        requireCatalogueAccess();
        CoverImageDto cover = courseCategoryService.getCoverImageUrl(
                securityContext.getTenantId(),
                id
        );
        return Response.ok(cover).build();
    }

    @POST
    @Path("/{id}/videos/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload a private lesson video to Cloudflare R2")
    public Response uploadVideo(
            @PathParam("id") String id,
            @RestForm("file") FileUpload file,
            @RestForm("title") String title
    ) throws Exception {
        requireCourseManage();
        if (file == null || file.size() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Video file is required"))
                    .build();
        }

        try (InputStream data = Files.newInputStream(file.uploadedFile())) {
            LessonVideoDto uploaded = courseLessonService.uploadR2Video(
                    securityContext.getTenantId(),
                    id,
                    securityContext.getUserId(),
                    title,
                    file.fileName(),
                    file.contentType(),
                    file.size(),
                    data
            );
            return Response.status(Response.Status.CREATED).entity(uploaded).build();
        }
    }

    @GET
    @Path("/{id}/videos/{videoId}/playback")
    @Operation(summary = "Get a short-lived signed URL to play an R2 video")
    public Response playback(@PathParam("id") String id, @PathParam("videoId") String videoId) {
        requireCourseRead();
        VideoPlaybackDto playback = courseLessonService.getPlaybackUrl(
                securityContext.getTenantId(),
                id,
                videoId,
                securityContext.getUserId(),
                canManageCourse()
        );
        return Response.ok(playback).build();
    }

    @POST
    @Path("/{id}/slides/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload a private PDF/PPTX slide deck to Cloudflare R2")
    public Response uploadSlide(
            @PathParam("id") String id,
            @RestForm("file") FileUpload file,
            @RestForm("title") String title
    ) throws Exception {
        requireCourseManage();
        if (file == null || file.size() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Slide file is required"))
                    .build();
        }

        try (InputStream data = Files.newInputStream(file.uploadedFile())) {
            LessonSlideDto uploaded = courseLessonService.uploadR2Slide(
                    securityContext.getTenantId(),
                    id,
                    securityContext.getUserId(),
                    title,
                    file.fileName(),
                    file.contentType(),
                    file.size(),
                    data
            );
            return Response.status(Response.Status.CREATED).entity(uploaded).build();
        }
    }

    @GET
    @Path("/{id}/slides/{slideId}/view")
    @Operation(summary = "Get a short-lived signed URL to view an R2 slide deck")
    public Response viewSlide(@PathParam("id") String id, @PathParam("slideId") String slideId) {
        requireCourseRead();
        SlideViewDto view = courseLessonService.getSlideViewUrl(
                securityContext.getTenantId(),
                id,
                slideId,
                securityContext.getUserId(),
                canManageCourse()
        );
        return Response.ok(view).build();
    }

    @POST
    @Path("/{id}/documents/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload a private document (PDF, Word, etc.) to Cloudflare R2")
    public Response uploadDocument(
            @PathParam("id") String id,
            @RestForm("file") FileUpload file,
            @RestForm("title") String title
    ) throws Exception {
        requireCourseManage();
        if (file == null || file.size() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Document file is required"))
                    .build();
        }

        try (InputStream data = Files.newInputStream(file.uploadedFile())) {
            LessonDocumentDto uploaded = courseLessonService.uploadR2Document(
                    securityContext.getTenantId(),
                    id,
                    securityContext.getUserId(),
                    title,
                    file.fileName(),
                    file.contentType(),
                    file.size(),
                    data
            );
            return Response.status(Response.Status.CREATED).entity(uploaded).build();
        }
    }

    @GET
    @Path("/{id}/documents/{documentId}/view")
    @Operation(summary = "Get a short-lived signed URL to view an R2 document")
    public Response viewDocument(@PathParam("id") String id, @PathParam("documentId") String documentId) {
        requireCourseRead();
        DocumentViewDto view = courseLessonService.getDocumentViewUrl(
                securityContext.getTenantId(),
                id,
                documentId,
                securityContext.getUserId(),
                canManageCourse()
        );
        return Response.ok(view).build();
    }

    @GET
    @Path("/{id}/lesson")
    @Operation(summary = "Get lesson content (notes, slides, videos)")
    public Response getLesson(@PathParam("id") String id) {
        requireCourseRead();
        var category = courseCategoryService.getEntity(securityContext.getTenantId(), id);
        if (!courseSubscriptionService.canViewContent(
                securityContext.getTenantId(),
                securityContext.getUserId(),
                category,
                canManageCourse()
        )) {
            throw new BusinessException(402, "Subscribe to this programme, year, semester, or course unit to open the outline.");
        }
        CourseLessonDto lesson = courseLessonService.getLesson(
                securityContext.getTenantId(),
                id,
                canManageCourse()
        );
        return Response.ok(lesson).build();
    }

    @PUT
    @Path("/{id}/lesson")
    @Operation(summary = "Save lesson content (notes, slides, videos) as a staff draft")
    public Response saveLesson(@PathParam("id") String id, @Valid SaveCourseLessonRequest request) {
        requireEdit(id);
        CourseLessonDto saved = courseLessonService.saveLesson(
                securityContext.getTenantId(),
                id,
                request,
                securityContext.getUserId()
        );
        return Response.ok(saved).build();
    }

    @POST
    @Path("/{id}/lesson/publish")
    @Operation(summary = "Publish drafted lesson notes so students can see them")
    public Response publishLesson(@PathParam("id") String id) {
        requireEdit(id);
        CourseLessonDto published = courseLessonService.publishLesson(
                securityContext.getTenantId(),
                id,
                securityContext.getUserId()
        );
        return Response.ok(published).build();
    }
}
