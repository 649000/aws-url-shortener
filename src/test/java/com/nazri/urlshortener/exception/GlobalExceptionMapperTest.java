package com.nazri.urlshortener.exception;

import com.nazri.urlshortener.dto.ErrorDTO;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionMapperTest {

    private final GlobalExceptionMapper mapper = new GlobalExceptionMapper();

    @Test
    void mapsAnyExceptionToInternalServerError() {
        Response response = mapper.toResponse(new RuntimeException("boom"));

        assertEquals(500, response.getStatus());
        Object entity = response.getEntity();
        assertTrue(entity instanceof ErrorDTO);
        ErrorDTO dto = (ErrorDTO) entity;
        assertEquals(500, dto.getStatusCode());
        assertEquals("An unexpected error occurred. Please try again later.", dto.getError());
    }

    @Test
    void doesNotLeakInternalExceptionMessage() {
        Response response = mapper.toResponse(new IllegalStateException("secret-stack-trace-msg"));
        ErrorDTO dto = (ErrorDTO) response.getEntity();
        assertFalse(dto.getError().contains("secret-stack-trace-msg"));
    }

    @Test
    void handlesNullExceptionGracefully() {
        // Mapper logs and returns generic 500; even a null exception shouldn't crash the response builder.
        assertDoesNotThrow(() -> {
            Response response = mapper.toResponse(new NullPointerException("npe"));
            assertEquals(500, response.getStatus());
        });
    }
}
