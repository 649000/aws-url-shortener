package com.nazri.urlshortener.dto;

public class ShortenResponseDTO {

    private String shortUrlId;

    public ShortenResponseDTO() {
    }

    public ShortenResponseDTO(String shortUrlId) {
        this.shortUrlId = shortUrlId;
    }

    public String getShortUrlId() {
        return shortUrlId;
    }

    public void setShortUrlId(String shortUrlId) {
        this.shortUrlId = shortUrlId;
    }

    @Override
    public String toString() {
        return "ShortenResponseDTO{shortUrlId='" + shortUrlId + "'}";
    }
}
