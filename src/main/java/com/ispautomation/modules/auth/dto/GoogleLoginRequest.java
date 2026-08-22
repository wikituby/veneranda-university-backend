package com.ispautomation.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Google Identity Services ID token from the frontend.
 */
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
