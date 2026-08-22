package com.ispautomation.modules.course.dto;

public class LessonVideoDto {

    private String id;
    private String title;
    private String url;
    private String provider;
    private Integer orderIndex;
    /** True when playback must go through the signed URL API (Cloudflare R2). */
    private boolean signedPlayback;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean isSignedPlayback() {
        return signedPlayback;
    }

    public void setSignedPlayback(boolean signedPlayback) {
        this.signedPlayback = signedPlayback;
    }
}
