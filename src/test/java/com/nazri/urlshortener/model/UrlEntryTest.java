package com.nazri.urlshortener.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlEntryTest {

    @Test
    void gettersAndSetters() {
        UrlEntry entry = new UrlEntry();
        entry.setId("abc");
        entry.setOriginalUrl("https://example.com");
        entry.setUserId("user-1");
        entry.setCreationDate(1L);
        entry.setLastAccessedDate(2L);
        entry.setAccessCount(5L);
        entry.setExpirationDate(3L);
        entry.setCustomAlias("alias");
        entry.setStatus(UrlStatus.ACTIVE);

        assertEquals("abc", entry.getId());
        assertEquals("https://example.com", entry.getOriginalUrl());
        assertEquals("user-1", entry.getUserId());
        assertEquals(1L, entry.getCreationDate());
        assertEquals(2L, entry.getLastAccessedDate());
        assertEquals(5L, entry.getAccessCount());
        assertEquals(3L, entry.getExpirationDate());
        assertEquals("alias", entry.getCustomAlias());
        assertEquals(UrlStatus.ACTIVE, entry.getStatus());
    }

    @Test
    void toStringContainsAllFields() {
        UrlEntry entry = new UrlEntry();
        entry.setId("abc");
        entry.setOriginalUrl("https://example.com");
        entry.setUserId("user");
        entry.setStatus(UrlStatus.ACTIVE);

        String s = entry.toString();
        assertTrue(s.contains("abc"));
        assertTrue(s.contains("https://example.com"));
        assertTrue(s.contains("user"));
        assertTrue(s.contains("ACTIVE"));
    }
}
