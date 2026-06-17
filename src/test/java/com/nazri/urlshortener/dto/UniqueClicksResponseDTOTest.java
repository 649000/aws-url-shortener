package com.nazri.urlshortener.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UniqueClicksResponseDTOTest {

    @Test
    void constructorAndGetter() {
        UniqueClicksResponseDTO dto = new UniqueClicksResponseDTO(42L);
        assertEquals(42L, dto.getUniqueClicks());
    }

    @Test
    void noArgAndSetter() {
        UniqueClicksResponseDTO dto = new UniqueClicksResponseDTO();
        dto.setUniqueClicks(7L);
        assertEquals(7L, dto.getUniqueClicks());
    }

    @Test
    void toStringIncludesCount() {
        assertTrue(new UniqueClicksResponseDTO(99L).toString().contains("99"));
    }
}
