package com.nazri.urlshortener.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorDTOTest {

    @Test
    void threeArgConstructorPopulatesAllFields() {
        ErrorDTO dto = new ErrorDTO("oops", "details here", 400);
        assertEquals("oops", dto.getError());
        assertEquals("details here", dto.getDetails());
        assertEquals(400, dto.getStatusCode());
    }

    @Test
    void twoArgConstructorLeavesDetailsNull() {
        ErrorDTO dto = new ErrorDTO("oops", 500);
        assertEquals("oops", dto.getError());
        assertNull(dto.getDetails());
        assertEquals(500, dto.getStatusCode());
    }

    @Test
    void noArgConstructorAndSetters() {
        ErrorDTO dto = new ErrorDTO();
        dto.setError("e");
        dto.setDetails("d");
        dto.setStatusCode(404);
        assertEquals("e", dto.getError());
        assertEquals("d", dto.getDetails());
        assertEquals(404, dto.getStatusCode());
    }

    @Test
    void equalsAndHashCodeRespectAllFields() {
        ErrorDTO a = new ErrorDTO("e", "d", 400);
        ErrorDTO b = new ErrorDTO("e", "d", 400);
        ErrorDTO c = new ErrorDTO("e", "different", 400);
        ErrorDTO d = new ErrorDTO("e", "d", 401);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, null);
        assertNotEquals(a, "string");
    }

    @Test
    void toStringIncludesAllFields() {
        String s = new ErrorDTO("oops", "details", 500).toString();
        assertTrue(s.contains("oops"));
        assertTrue(s.contains("details"));
        assertTrue(s.contains("500"));
    }
}
