package com.ispautomation.modules.course.dto;

import java.util.ArrayList;
import java.util.List;

public class CourseLessonDto {

    private String categoryId;
    private String title;
    private String description;
    private String notesBody;
    /** True when the staff draft differs from the published notes (editors only). */
    private boolean hasUnpublishedNotes;
    private List<LessonSlideDto> slides = new ArrayList<>();
    private List<LessonVideoDto> videos = new ArrayList<>();
    private List<LessonDocumentDto> documents = new ArrayList<>();
    private List<LessonLiveSessionDto> liveSessions = new ArrayList<>();

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotesBody() {
        return notesBody;
    }

    public void setNotesBody(String notesBody) {
        this.notesBody = notesBody;
    }

    public boolean getHasUnpublishedNotes() {
        return hasUnpublishedNotes;
    }

    public void setHasUnpublishedNotes(boolean hasUnpublishedNotes) {
        this.hasUnpublishedNotes = hasUnpublishedNotes;
    }

    public List<LessonSlideDto> getSlides() {
        return slides;
    }

    public void setSlides(List<LessonSlideDto> slides) {
        this.slides = slides;
    }

    public List<LessonVideoDto> getVideos() {
        return videos;
    }

    public void setVideos(List<LessonVideoDto> videos) {
        this.videos = videos;
    }

    public List<LessonDocumentDto> getDocuments() {
        return documents;
    }

    public void setDocuments(List<LessonDocumentDto> documents) {
        this.documents = documents;
    }

    public List<LessonLiveSessionDto> getLiveSessions() {
        return liveSessions;
    }

    public void setLiveSessions(List<LessonLiveSessionDto> liveSessions) {
        this.liveSessions = liveSessions;
    }
}
