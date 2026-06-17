package com.nazri.urlshortener.service;

import com.nazri.urlshortener.model.ClickEvent;
import com.nazri.urlshortener.model.UrlEntry;
import com.nazri.urlshortener.model.UrlStatus;
import com.nazri.urlshortener.repository.ClickEventRepository;
import com.nazri.urlshortener.repository.UrlEntryRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class UrlShortenerService {

    static final int ID_LENGTH = 12;
    static final int MAX_ID_GENERATION_ATTEMPTS = 5;
    private static final String BASE62_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final UrlEntryRepository urlEntryRepository;
    private final ClickEventRepository clickEventRepository;

    public UrlShortenerService(UrlEntryRepository urlEntryRepository,
                               ClickEventRepository clickEventRepository) {
        this.urlEntryRepository = urlEntryRepository;
        this.clickEventRepository = clickEventRepository;
    }

    public String shortenUrl(String originalUrl, String userId, Long expirationDate, String customAlias) {
        if (originalUrl == null || originalUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is required and cannot be empty");
        }
        if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }

        String id = (customAlias != null && !customAlias.isEmpty())
                ? customAlias
                : generateUniqueId();

        if (urlEntryRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Short URL ID already in use: " + id);
        }

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
        urlEntry.setStatus(UrlStatus.ACTIVE);

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

        if (optionalUrlEntry.isEmpty()) {
            return Optional.empty();
        }

        UrlEntry urlEntry = optionalUrlEntry.get();
        long now = Instant.now().getEpochSecond();

        if (!UrlStatus.ACTIVE.equals(urlEntry.getStatus())) {
            return Optional.empty();
        }
        if (urlEntry.getExpirationDate() != null && urlEntry.getExpirationDate() <= now) {
            return Optional.empty();
        }

        // Atomic increment; avoids the read-modify-write race in the original code.
        urlEntryRepository.incrementAccessCount(id, now);
        recordClickEvent(id, now, ipAddress, userAgent, referrer);
        return Optional.of(urlEntry.getOriginalUrl());
    }

    private void recordClickEvent(String shortUrlId, long timestamp,
                                  String ipAddress, String userAgent, String referrer) {
        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setShortUrlId(shortUrlId);
        clickEvent.setTimestamp(timestamp);
        clickEvent.setIpAddress(ipAddress);
        clickEvent.setUserAgent(userAgent);
        clickEvent.setReferrer(referrer);
        // Geo-IP lookup is intentionally out of scope; tracked as "Unknown".
        clickEvent.setCountry("Unknown");
        clickEvent.setCity("Unknown");
        clickEventRepository.save(clickEvent);
    }

    /**
     * Generates a collision-resistant short ID by encoding a UUID v4 as base62 and
     * truncating to {@link #ID_LENGTH} characters (≈ 71 bits of entropy → birthday-paradox
     * collisions only become likely past ~46B entries). Retries up to
     * {@link #MAX_ID_GENERATION_ATTEMPTS} times on collision before failing.
     *
     * <p>Package-private for unit testing.
     */
    String generateUniqueId() {
        for (int attempt = 0; attempt < MAX_ID_GENERATION_ATTEMPTS; attempt++) {
            String candidate = toBase62(UUID.randomUUID(), ID_LENGTH);
            if (urlEntryRepository.findById(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique short URL ID after "
                        + MAX_ID_GENERATION_ATTEMPTS + " attempts");
    }

    /**
     * Encodes the first {@code length} base62 digits of a UUID v4.
     * Visible for testing.
     */
    static String toBase62(UUID uuid, int length) {
        byte[] bytes = new byte[16];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        BigInteger value = new BigInteger(1, bytes);
        BigInteger base = BigInteger.valueOf(62);
        StringBuilder sb = new StringBuilder(length);
        while (value.signum() > 0 && sb.length() < length) {
            BigInteger[] divRem = value.divideAndRemainder(base);
            sb.append(BASE62_ALPHABET.charAt(divRem[1].intValue()));
            value = divRem[0];
        }
        while (sb.length() < length) {
            sb.append('0');
        }
        return sb.reverse().toString();
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
