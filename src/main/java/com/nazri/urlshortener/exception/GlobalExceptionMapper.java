package com.nazri.urlshortener.exception;

import com.nazri.urlshortener.dto.ErrorDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Logger;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger logger = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        logger.severe("Unexpected error occurred: " + exception.getMessage());
        exception.printStackTrace();

        ErrorDTO errorDTO = new ErrorDTO("An unexpected error occurred. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorDTO)
                .build();
    }
}


