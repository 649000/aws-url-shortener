package com.nazri.urlshortener.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShortenResponseDTOTest {

    @Test
    void twoArgConstructorSetsField() {
        ShortenResponseDTO dto = new ShortenResponseDTO("abc123");
        assertEquals("abc123", dto.getShortUrlId());
    }

    @Test
    void noArgConstructorAndSetter() {
        ShortenResponseDTO dto = new ShortenResponseDTO();
        dto.setShortUrlId("xyz");
        assertEquals("xyz", dto.getShortUrlId());
    }

    @Test
    void toStringIncludesId() {
        assertTrue(new ShortenResponseDTO("abc").toString().contains("abc"));
    }
}
