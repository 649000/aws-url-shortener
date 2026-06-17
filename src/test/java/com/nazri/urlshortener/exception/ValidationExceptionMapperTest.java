package com.nazri.urlshortener.exception;

import com.nazri.urlshortener.dto.ErrorDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationExceptionMapperTest {

    private final ValidationExceptionMapper mapper = new ValidationExceptionMapper();

    @Test
    void mapsConstraintViolationsToBadRequest() {
        ConstraintViolation<?> v1 = mockViolation("url", "must not be blank");
        ConstraintViolation<?> v2 = mockViolation("customAlias", "invalid pattern");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(v1, v2));

        Response response = mapper.toResponse(ex);

        assertEquals(400, response.getStatus());
        ErrorDTO dto = (ErrorDTO) response.getEntity();
        assertEquals(400, dto.getStatusCode());
        assertTrue(dto.getError().contains("url: must not be blank"));
        assertTrue(dto.getError().contains("customAlias: invalid pattern"));
        assertTrue(dto.getError().contains(", "));
    }

    @Test
    void emptyViolationsProducesEmptyMessage() {
        ConstraintViolationException ex = new ConstraintViolationException(Set.of());
        Response response = mapper.toResponse(ex);
        assertEquals(400, response.getStatus());
        ErrorDTO dto = (ErrorDTO) response.getEntity();
        assertEquals("", dto.getError());
    }

    @SuppressWarnings("unchecked")
    private ConstraintViolation<?> mockViolation(String property, String message) {
        ConstraintViolation<Object> v = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(property);
        when(v.getPropertyPath()).thenReturn(path);
        when(v.getMessage()).thenReturn(message);
        return v;
    }
}
