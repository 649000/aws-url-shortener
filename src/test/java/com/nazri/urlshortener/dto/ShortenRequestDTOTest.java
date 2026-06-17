package com.nazri.urlshortener.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShortenRequestDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void validRequestProducesNoViolations() {
        ShortenRequestDTO dto = new ShortenRequestDTO();
        dto.setUrl("https://example.com");
        dto.setUserId("user-1");
        dto.setExpirationDate(123456789L);
        dto.setCustomAlias("my-alias");
        Set<ConstraintViolation<ShortenRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankUrlIsRejected() {
        ShortenRequestDTO dto = new ShortenRequestDTO();
        dto.setUrl("   ");
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void invalidSchemeIsRejected() {
        ShortenRequestDTO dto = new ShortenRequestDTO();
        dto.setUrl("javascript:alert(1)");
        Set<ConstraintViolation<ShortenRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("valid protocol")));
    }

    @Test
    void httpAndHttpsAccepted() {
        for (String url : new String[]{"http://example.com", "https://example.com", "ftp://x", "mailto:x@y"}) {
            ShortenRequestDTO dto = new ShortenRequestDTO();
            dto.setUrl(url);
            assertTrue(validator.validate(dto).isEmpty(), "should accept " + url);
        }
    }

    @Test
    void userIdOver255IsRejected() {
        ShortenRequestDTO dto = new ShortenRequestDTO();
        dto.setUrl("https://example.com");
        dto.setUserId("a".repeat(256));
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void customAliasOver50IsRejected() {
        ShortenRequestDTO dto = new ShortenRequestDTO();
        dto.setUrl("https://example.com");
        dto.setCustomAlias("a".repeat(51));
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void customAliasInvalidCharsRejected() {
        ShortenRequestDTO dto = new ShortenRequestDTO();
        dto.setUrl("https://example.com");
        dto.setCustomAlias("has space!");
        Set<ConstraintViolation<ShortenRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("alphanumeric")));
    }

    @Test
    void nullExpirationDateIsAllowed() {
        ShortenRequestDTO dto = new ShortenRequestDTO();
        dto.setUrl("https://example.com");
        dto.setExpirationDate(null);
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void toStringContainsAllFields() {
        ShortenRequestDTO dto = new ShortenRequestDTO();
        dto.setUrl("https://x");
        dto.setUserId("u");
        dto.setExpirationDate(1L);
        dto.setCustomAlias("a");
        String s = dto.toString();
        assertTrue(s.contains("https://x"));
        assertTrue(s.contains("u"));
        assertTrue(s.contains("a"));
    }
}
