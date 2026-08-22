package com.ispautomation.modules.course.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveCourseLessonRequest {

    private String notesBody;
    private List<LessonSlideDto> slides = new ArrayList<>();
    private List<LessonVideoDto> videos = new ArrayList<>();
    private List<LessonDocumentDto> documents = new ArrayList<>();
    private List<LessonLiveSessionDto> liveSessions = new ArrayList<>();

    public String getNotesBody() {
        return notesBody;
    }

    public void setNotesBody(String notesBody) {
        this.notesBody = notesBody;
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
