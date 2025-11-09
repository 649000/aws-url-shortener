package com.nazri.urlshortener.exception;

import com.nazri.urlshortener.dto.ErrorDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Logger;
import java.util.stream.Collectors;

@Provider
class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    private static final Logger logger = Logger.getLogger(ValidationExceptionMapper.class.getName());

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        logger.warning("Validation error occurred: " + exception.getMessage());

        // Extract validation error messages
        String errorMessage = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        ErrorDTO errorDTO = new ErrorDTO(errorMessage, Response.Status.BAD_REQUEST.getStatusCode());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDTO)
                .build();
    }
}