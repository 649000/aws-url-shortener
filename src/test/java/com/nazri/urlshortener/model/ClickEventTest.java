package com.nazri.urlshortener.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClickEventTest {

    @Test
    void gettersAndSetters() {
        ClickEvent event = new ClickEvent();
        event.setShortUrlId("abc");
        event.setTimestamp(123L);
        event.setIpAddress("1.1.1.1");
        event.setUserAgent("Mozilla");
        event.setReferrer("https://ref");
        event.setCountry("SG");
        event.setCity("Singapore");

        assertEquals("abc", event.getShortUrlId());
        assertEquals(123L, event.getTimestamp());
        assertEquals("1.1.1.1", event.getIpAddress());
        assertEquals("Mozilla", event.getUserAgent());
        assertEquals("https://ref", event.getReferrer());
        assertEquals("SG", event.getCountry());
        assertEquals("Singapore", event.getCity());
    }

    @Test
    void toStringContainsAllFields() {
        ClickEvent event = new ClickEvent();
        event.setShortUrlId("abc");
        event.setTimestamp(1L);
        event.setIpAddress("ip");
        event.setUserAgent("ua");
        event.setReferrer("ref");
        event.setCountry("c");
        event.setCity("ct");

        String s = event.toString();
        assertTrue(s.contains("abc"));
        assertTrue(s.contains("ua"));
        assertTrue(s.contains("ref"));
    }
}
