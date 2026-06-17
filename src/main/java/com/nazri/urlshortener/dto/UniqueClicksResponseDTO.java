package com.nazri.urlshortener.dto;

public class UniqueClicksResponseDTO {

    private long uniqueClicks;

    public UniqueClicksResponseDTO() {
    }

    public UniqueClicksResponseDTO(long uniqueClicks) {
        this.uniqueClicks = uniqueClicks;
    }

    public long getUniqueClicks() {
        return uniqueClicks;
    }

    public void setUniqueClicks(long uniqueClicks) {
        this.uniqueClicks = uniqueClicks;
    }

    @Override
    public String toString() {
        return "UniqueClicksResponseDTO{uniqueClicks=" + uniqueClicks + '}';
    }
}
