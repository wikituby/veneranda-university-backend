package com.ispautomation.modules.course.dto;

import jakarta.validation.constraints.NotBlank;

public class UnenrollRequest {

    @NotBlank(message = "Password is required to confirm")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
