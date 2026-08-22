package com.ispautomation.modules.course.entity;

import com.ispautomation.common.entity.TenantAwareEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "course_lesson_contents")
public class CourseLessonContent extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CourseCategory category;

    @Column(name = "notes_body", columnDefinition = "TEXT")
    private String notesBody;

    @Column(name = "notes_draft_body", columnDefinition = "TEXT")
    private String notesDraftBody;

    @Column(name = "slides_draft_json", columnDefinition = "TEXT")
    private String slidesDraftJson;

    @Column(name = "videos_draft_json", columnDefinition = "TEXT")
    private String videosDraftJson;

    @Column(name = "documents_draft_json", columnDefinition = "TEXT")
    private String documentsDraftJson;

    @Column(name = "live_sessions_draft_json", columnDefinition = "TEXT")
    private String liveSessionsDraftJson;

    public CourseCategory getCategory() {
        return category;
    }

    public void setCategory(CourseCategory category) {
        this.category = category;
    }

    public String getNotesBody() {
        return notesBody;
    }

    public void setNotesBody(String notesBody) {
        this.notesBody = notesBody;
    }

    public String getNotesDraftBody() {
        return notesDraftBody;
    }

    public void setNotesDraftBody(String notesDraftBody) {
        this.notesDraftBody = notesDraftBody;
    }

    public String getSlidesDraftJson() {
        return slidesDraftJson;
    }

    public void setSlidesDraftJson(String slidesDraftJson) {
        this.slidesDraftJson = slidesDraftJson;
    }

    public String getVideosDraftJson() {
        return videosDraftJson;
    }

    public void setVideosDraftJson(String videosDraftJson) {
        this.videosDraftJson = videosDraftJson;
    }

    public String getDocumentsDraftJson() {
        return documentsDraftJson;
    }

    public void setDocumentsDraftJson(String documentsDraftJson) {
        this.documentsDraftJson = documentsDraftJson;
    }

    public String getLiveSessionsDraftJson() {
        return liveSessionsDraftJson;
    }

    public void setLiveSessionsDraftJson(String liveSessionsDraftJson) {
        this.liveSessionsDraftJson = liveSessionsDraftJson;
    }
}
