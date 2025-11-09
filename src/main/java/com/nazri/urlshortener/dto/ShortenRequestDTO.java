package com.nazri.urlshortener.dto;

public class ShortenRequestDTO {
    private String url;
    private String userId;
    private Long expirationDate;
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
