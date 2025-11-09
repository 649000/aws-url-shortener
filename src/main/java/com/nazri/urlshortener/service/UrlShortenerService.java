package com.nazri.urlshortener.service;

import com.nazri.urlshortener.model.UrlEntry;

import com.nazri.urlshortener.repository.ClickEventRepository;
import com.nazri.urlshortener.repository.UrlEntryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.nazri.urlshortener.model.ClickEvent;

@ApplicationScoped
public class UrlShortenerService {

    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_URL_LENGTH = 8;

    @Inject
    UrlEntryRepository urlEntryRepository;

    @Inject
    ClickEventRepository clickEventRepository;

    public String shortenUrl(String originalUrl, String userId, Long expirationDate, String customAlias) {
        // Validate URL
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is required and cannot be empty");
        }
        
        if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
        
        String id = (customAlias != null && !customAlias.isEmpty()) ? customAlias : generateUniqueId();
        long now = Instant.now().getEpochSecond();

        UrlEntry urlEntry = new UrlEntry();
        urlEntry.setId(id);
        urlEntry.setOriginalUrl(originalUrl);
        urlEntry.setUserId(userId);
        urlEntry.setCreationDate(now);
        urlEntry.setLastAccessedDate(now);
        urlEntry.setAccessCount(0L);
        urlEntry.setExpirationDate(expirationDate);
        urlEntry.setCustomAlias(customAlias);
        urlEntry.setStatus("ACTIVE"); // Default status
        urlEntryRepository.save(urlEntry);
        return id;
    }
    
    public void validateUrlId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("URL ID is required and cannot be empty");
        }
    }
    
    public void validateTimeRange(Long startTime, Long endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime parameters are required");
        }
        
        if (startTime >= endTime) {
            throw new IllegalArgumentException("startTime must be less than endTime");
        }
    }

    public Optional<String> retrieveOriginalUrl(String id, String ipAddress, String userAgent, String referrer) {
        Optional<UrlEntry> optionalUrlEntry = urlEntryRepository.findById(id);

        if (optionalUrlEntry.isPresent()) {
            UrlEntry urlEntry = optionalUrlEntry.get();
            long now = Instant.now().getEpochSecond();
            if ("ACTIVE".equals(urlEntry.getStatus()) && 
                (urlEntry.getExpirationDate() == null || urlEntry.getExpirationDate() > now)) {
                urlEntry.setAccessCount(urlEntry.getAccessCount() + 1);
                urlEntry.setLastAccessedDate(now);
                urlEntryRepository.update(urlEntry);
                recordClickEvent(id, now, ipAddress, userAgent, referrer);
                return Optional.of(urlEntry.getOriginalUrl());
            } else {
                // URL is expired or inactive
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void recordClickEvent(String shortUrlId, long timestamp, String ipAddress, String userAgent, String referrer) {
        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setShortUrlId(shortUrlId);
        clickEvent.setTimestamp(timestamp);
        clickEvent.setIpAddress(ipAddress);
        clickEvent.setUserAgent(userAgent);
        clickEvent.setReferrer(referrer);
        // Placeholder for geo-IP lookup
        clickEvent.setCountry("Unknown"); 
        clickEvent.setCity("Unknown");
        clickEventRepository.save(clickEvent);
    }

    private String generateUniqueId() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < SHORT_URL_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    public List<ClickEvent> getClickEventsForUrl(String shortUrlId) {
        return clickEventRepository.findByShortUrlId(shortUrlId);
    }

    public long getUniqueClicks(String shortUrlId) {
        return getClickEventsForUrl(shortUrlId).stream()
                .map(ClickEvent::getIpAddress)
                .distinct()
                .count();
    }

    public Map<String, Long> getClicksByCountry(String shortUrlId) {
        return getClickEventsForUrl(shortUrlId).stream()
                .collect(Collectors.groupingBy(ClickEvent::getCountry, Collectors.counting()));
    }

    public Map<String, Long> getClicksByReferrer(String shortUrlId) {
        return getClickEventsForUrl(shortUrlId).stream()
                .filter(click -> click.getReferrer() != null && !click.getReferrer().isEmpty())
                .collect(Collectors.groupingBy(ClickEvent::getReferrer, Collectors.counting()));
    }

    public Map<String, Long> getClicksByUserAgent(String shortUrlId) {
        return getClickEventsForUrl(shortUrlId).stream()
                .filter(click -> click.getUserAgent() != null && !click.getUserAgent().isEmpty())
                .collect(Collectors.groupingBy(ClickEvent::getUserAgent, Collectors.counting()));
    }

    public List<ClickEvent> getClickEventsInTimeRange(String shortUrlId, Long startTime, Long endTime) {
        return clickEventRepository.findByShortUrlIdAndTimeRange(shortUrlId, startTime, endTime);
    }
}
