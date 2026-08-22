package com.ispautomation.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token request payload.
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}