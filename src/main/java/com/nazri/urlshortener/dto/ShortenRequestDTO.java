package com.nazri.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ShortenRequestDTO {
    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^(https?|ftp|mailto|tel):.*", message = "URL must be a valid protocol (http, https, ftp, mailto, tel)")
    private String url;
    
    @Size(max = 255, message = "User ID must not exceed 255 characters")
    private String userId;
    
    @NotNull(message = "Expiration date is required")
    private Long expirationDate;
    
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Custom alias can only contain alphanumeric characters, hyphens, and underscores")
    @Size(max = 50, message = "Custom alias must not exceed 50 characters")
    private String customAlias;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Long expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    @Override
    public String toString() {
        return "ShortenRequestDTO{" +
                "url='" + url + '\'' +
                ", userId='" + userId + '\'' +
                ", expirationDate=" + expirationDate +
                ", customAlias='" + customAlias + '\'' +
                '}';
    }
}
