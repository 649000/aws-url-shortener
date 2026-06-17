package com.nazri.urlshortener.service;

import com.nazri.urlshortener.model.ClickEvent;
import com.nazri.urlshortener.model.UrlEntry;
import com.nazri.urlshortener.model.UrlStatus;
import com.nazri.urlshortener.repository.ClickEventRepository;
import com.nazri.urlshortener.repository.UrlEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    UrlEntryRepository urlEntryRepository;

    @Mock
    ClickEventRepository clickEventRepository;

    UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(urlEntryRepository, clickEventRepository);
    }

    @Nested
    @DisplayName("shortenUrl()")
    class ShortenUrl {

        @Test
        void rejectsNullUrl() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.shortenUrl(null, "user", null, null));
            assertEquals("URL is required and cannot be empty", ex.getMessage());
        }

        @Test
        void rejectsBlankUrl() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.shortenUrl("   ", "user", null, null));
        }

        @Test
        void rejectsNonHttpScheme() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.shortenUrl("ftp://example.com", "user", null, null));
            assertThrows(IllegalArgumentException.class,
                    () -> service.shortenUrl("not-a-url", "user", null, null));
        }

        @Test
        void acceptsHttpAndHttps() {
            when(urlEntryRepository.findById(anyString())).thenReturn(Optional.empty());
            assertDoesNotThrow(() -> service.shortenUrl("http://example.com", null, null, null));
            assertDoesNotThrow(() -> service.shortenUrl("https://example.com", null, null, null));
        }

        @Test
        void usesCustomAliasWhenProvided() {
            when(urlEntryRepository.findById("myAlias")).thenReturn(Optional.empty());
            String id = service.shortenUrl("https://example.com", "user", null, "myAlias");
            assertEquals("myAlias", id);
            verify(urlEntryRepository, times(1)).findById("myAlias");
        }

        @Test
        void generatesShortIdWhenNoCustomAlias() {
            when(urlEntryRepository.findById(anyString())).thenReturn(Optional.empty());
            String id = service.shortenUrl("https://example.com", "user", null, null);
            assertNotNull(id);
            assertEquals(UrlShortenerService.ID_LENGTH, id.length());
            assertTrue(id.matches("[0-9A-Za-z]+"));
        }

        @Test
        void rejectsAlreadyUsedCustomAlias() {
            when(urlEntryRepository.findById("taken")).thenReturn(Optional.of(new UrlEntry()));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.shortenUrl("https://example.com", "user", null, "taken"));
            assertTrue(ex.getMessage().contains("already in use"));
        }

        @Test
        void savesEntryWithExpectedFields() {
            when(urlEntryRepository.findById(anyString())).thenReturn(Optional.empty());
            long before = Instant.now().getEpochSecond();

            String id = service.shortenUrl("https://example.com", "user-1", 123456789L, null);

            ArgumentCaptor<UrlEntry> captor = ArgumentCaptor.forClass(UrlEntry.class);
            verify(urlEntryRepository).save(captor.capture());
            UrlEntry saved = captor.getValue();
            assertEquals(id, saved.getId());
            assertEquals("https://example.com", saved.getOriginalUrl());
            assertEquals("user-1", saved.getUserId());
            assertEquals(123456789L, saved.getExpirationDate());
            assertEquals(UrlStatus.ACTIVE, saved.getStatus());
            assertEquals(0L, saved.getAccessCount());
            assertTrue(saved.getCreationDate() >= before);
        }

        @Test
        void retriesOnIdCollision() {
            when(urlEntryRepository.findById(anyString()))
                    .thenReturn(Optional.of(new UrlEntry()))
                    .thenReturn(Optional.empty());

            String id = service.shortenUrl("https://example.com", null, null, null);
            assertNotNull(id);
        }

        @Test
        void givesUpAfterMaxAttempts() {
            when(urlEntryRepository.findById(anyString())).thenReturn(Optional.of(new UrlEntry()));
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.shortenUrl("https://example.com", null, null, null));
            assertTrue(ex.getMessage().contains("Failed to generate"));
        }
    }

    @Nested
    @DisplayName("validateUrlId()")
    class ValidateUrlId {
        @Test
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> service.validateUrlId(null));
        }

        @Test
        void rejectsBlank() {
            assertThrows(IllegalArgumentException.class, () -> service.validateUrlId(""));
            assertThrows(IllegalArgumentException.class, () -> service.validateUrlId("  "));
        }

        @Test
        void acceptsValid() {
            assertDoesNotThrow(() -> service.validateUrlId("abc123"));
        }
    }

    @Nested
    @DisplayName("validateTimeRange()")
    class ValidateTimeRange {
        @Test
        void rejectsNulls() {
            assertThrows(IllegalArgumentException.class, () -> service.validateTimeRange(null, 1L));
            assertThrows(IllegalArgumentException.class, () -> service.validateTimeRange(1L, null));
        }

        @Test
        void rejectsInverted() {
            assertThrows(IllegalArgumentException.class, () -> service.validateTimeRange(10L, 5L));
            assertThrows(IllegalArgumentException.class, () -> service.validateTimeRange(5L, 5L));
        }

        @Test
        void acceptsValid() {
            assertDoesNotThrow(() -> service.validateTimeRange(1L, 2L));
        }
    }

    @Nested
    @DisplayName("retrieveOriginalUrl()")
    class RetrieveOriginalUrl {

        @Test
        void emptyForMissingEntry() {
            when(urlEntryRepository.findById("missing")).thenReturn(Optional.empty());
            assertTrue(service.retrieveOriginalUrl("missing", "1.1.1.1", "ua", "ref").isEmpty());
            verify(urlEntryRepository, never()).incrementAccessCount(anyString(), anyLong());
        }

        @Test
        void emptyForInactiveEntry() {
            UrlEntry entry = activeEntry();
            entry.setStatus(UrlStatus.INACTIVE);
            when(urlEntryRepository.findById("id")).thenReturn(Optional.of(entry));
            assertTrue(service.retrieveOriginalUrl("id", "1.1.1.1", "ua", "ref").isEmpty());
            verify(urlEntryRepository, never()).incrementAccessCount(anyString(), anyLong());
        }

        @Test
        void emptyForExpiredEntry() {
            UrlEntry entry = activeEntry();
            entry.setExpirationDate(Instant.now().getEpochSecond() - 100);
            when(urlEntryRepository.findById("id")).thenReturn(Optional.of(entry));
            assertTrue(service.retrieveOriginalUrl("id", "1.1.1.1", "ua", "ref").isEmpty());
            verify(urlEntryRepository, never()).incrementAccessCount(anyString(), anyLong());
        }

        @Test
        void emptyForExpiredExplicitStatus() {
            UrlEntry entry = activeEntry();
            entry.setStatus(UrlStatus.EXPIRED);
            when(urlEntryRepository.findById("id")).thenReturn(Optional.of(entry));
            assertTrue(service.retrieveOriginalUrl("id", "1.1.1.1", "ua", "ref").isEmpty());
        }

        @Test
        void returnsUrlAndIncrementsForActive() {
            UrlEntry entry = activeEntry();
            when(urlEntryRepository.findById("id")).thenReturn(Optional.of(entry));

            Optional<String> result = service.retrieveOriginalUrl("id", "1.1.1.1", "ua", "ref");

            assertTrue(result.isPresent());
            assertEquals("https://example.com", result.get());
            verify(urlEntryRepository).incrementAccessCount(eq("id"), anyLong());
            verify(clickEventRepository).save(any(ClickEvent.class));
        }

        @Test
        void recordsClickEventWithExpectedFields() {
            UrlEntry entry = activeEntry();
            when(urlEntryRepository.findById("id")).thenReturn(Optional.of(entry));

            service.retrieveOriginalUrl("id", "10.0.0.1", "Mozilla", "https://ref.example");

            ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
            verify(clickEventRepository).save(captor.capture());
            ClickEvent event = captor.getValue();
            assertEquals("id", event.getShortUrlId());
            assertEquals("10.0.0.1", event.getIpAddress());
            assertEquals("Mozilla", event.getUserAgent());
            assertEquals("https://ref.example", event.getReferrer());
            assertEquals("Unknown", event.getCountry());
            assertEquals("Unknown", event.getCity());
        }

        @Test
        void neverExpiresWhenExpirationDateIsNull() {
            UrlEntry entry = activeEntry();
            entry.setExpirationDate(null);
            when(urlEntryRepository.findById("id")).thenReturn(Optional.of(entry));
            assertTrue(service.retrieveOriginalUrl("id", "1.1.1.1", "ua", "ref").isPresent());
        }
    }

    @Nested
    @DisplayName("Analytics delegations")
    class Analytics {

        @Test
        void getClickEventsDelegates() {
            when(clickEventRepository.findByShortUrlId("id")).thenReturn(List.of(new ClickEvent()));
            assertEquals(1, service.getClickEventsForUrl("id").size());
        }

        @Test
        void getUniqueClicksCountsDistinctIps() {
            ClickEvent a = new ClickEvent(); a.setIpAddress("1.1.1.1");
            ClickEvent b = new ClickEvent(); b.setIpAddress("1.1.1.1");
            ClickEvent c = new ClickEvent(); c.setIpAddress("2.2.2.2");
            when(clickEventRepository.findByShortUrlId("id")).thenReturn(List.of(a, b, c));
            assertEquals(2, service.getUniqueClicks("id"));
        }

        @Test
        void getUniqueClicksZeroOnEmpty() {
            when(clickEventRepository.findByShortUrlId("id")).thenReturn(List.of());
            assertEquals(0, service.getUniqueClicks("id"));
        }

        @Test
        void getClicksByCountryGroups() {
            ClickEvent a = new ClickEvent(); a.setCountry("SG");
            ClickEvent b = new ClickEvent(); b.setCountry("SG");
            ClickEvent c = new ClickEvent(); c.setCountry("US");
            when(clickEventRepository.findByShortUrlId("id")).thenReturn(List.of(a, b, c));
            Map<String, Long> result = service.getClicksByCountry("id");
            assertEquals(2L, result.get("SG"));
            assertEquals(1L, result.get("US"));
        }

        @Test
        void getClicksByReferrerIgnoresNullAndEmpty() {
            ClickEvent a = new ClickEvent(); a.setReferrer("https://a");
            ClickEvent b = new ClickEvent(); b.setReferrer(null);
            ClickEvent c = new ClickEvent(); c.setReferrer("");
            ClickEvent d = new ClickEvent(); d.setReferrer("https://a");
            when(clickEventRepository.findByShortUrlId("id")).thenReturn(List.of(a, b, c, d));
            Map<String, Long> result = service.getClicksByReferrer("id");
            assertEquals(1, result.size());
            assertEquals(2L, result.get("https://a"));
        }

        @Test
        void getClicksByUserAgentIgnoresNullAndEmpty() {
            ClickEvent a = new ClickEvent(); a.setUserAgent("Mozilla");
            ClickEvent b = new ClickEvent(); b.setUserAgent(null);
            ClickEvent c = new ClickEvent(); c.setUserAgent("");
            when(clickEventRepository.findByShortUrlId("id")).thenReturn(List.of(a, b, c));
            Map<String, Long> result = service.getClicksByUserAgent("id");
            assertEquals(1, result.size());
            assertEquals(1L, result.get("Mozilla"));
        }

        @Test
        void getClickEventsInTimeRangeDelegates() {
            when(clickEventRepository.findByShortUrlIdAndTimeRange("id", 1L, 2L))
                    .thenReturn(List.of(new ClickEvent()));
            assertEquals(1, service.getClickEventsInTimeRange("id", 1L, 2L).size());
        }
    }

    @Nested
    @DisplayName("toBase62()")
    class Base62 {

        @Test
        void encodesUuidToExpectedLength() {
            String encoded = UrlShortenerService.toBase62(UUID.randomUUID(), 12);
            assertEquals(12, encoded.length());
            assertTrue(encoded.matches("[0-9A-Za-z]+"));
        }

        @Test
        void zeroUuidPadsWithZeros() {
            String encoded = UrlShortenerService.toBase62(new UUID(0L, 0L), 12);
            assertEquals(12, encoded.length());
            assertEquals("000000000000", encoded);
        }
    }

    private UrlEntry activeEntry() {
        UrlEntry entry = new UrlEntry();
        entry.setId("id");
        entry.setOriginalUrl("https://example.com");
        entry.setStatus(UrlStatus.ACTIVE);
        entry.setAccessCount(0L);
        return entry;
    }
}
