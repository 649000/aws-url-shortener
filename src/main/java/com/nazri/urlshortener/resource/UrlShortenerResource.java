package com.nazri.urlshortener.resource;

import com.nazri.urlshortener.dto.ErrorDTO;
import com.nazri.urlshortener.dto.ShortenRequestDTO;
import com.nazri.urlshortener.service.UrlShortenerService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;

/**
 * Resource class for handling URL shortening and redirection operations.
 */
@Path("/")
public class UrlShortenerResource {

    private static final Logger logger = Logger.getLogger(UrlShortenerResource.class.getName());

    @Inject
    UrlShortenerService urlShortenerService;

    @Context
    HttpHeaders headers;

    /**
     * Creates a shortened URL from the provided original URL.
     * 
     * @param request The ShortenRequest containing the original URL and optional parameters
     * @return A Response containing the shortened URL ID
     */
    @POST
    @Path("shorten")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response shortenUrl(@Valid ShortenRequestDTO request) {
        try {
            // Placeholder for userId
            String userId = request.getUserId() != null && !request.getUserId().trim().isEmpty() ? request.getUserId() : "anonymous";
            
            String shortUrlId = urlShortenerService.shortenUrl(request.getUrl(), userId, request.getExpirationDate(), request.getCustomAlias());
            return Response.ok(String.format("{\"shortUrlId\": \"%s\"}", shortUrlId)).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorDTO)
                    .build();
        } catch (Exception e) {
            logger.severe("Error shortening URL: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO("Failed to shorten URL. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorDTO)
                    .build();
        }
    }

    /**
     * Retrieves the original URL for a given shortened URL ID and redirects to it.
     * 
     * @param id The ID of the shortened URL
     * @return A Response that either redirects to the original URL or returns an error
     */
    @GET
    @Path("{id}")
    public Response retrieveOriginalUrl(@PathParam("id") String id) {
        try {
            // Validate input
            urlShortenerService.validateUrlId(id);
            
            String ipAddress = headers.getHeaderString("X-Forwarded-For");
            if (ipAddress != null && ipAddress.contains(",")) {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = "Unknown"; 
            }
            String userAgent = headers.getHeaderString("User-Agent");
            String referrer = headers.getHeaderString("Referer");

            Optional<String> originalUrl = urlShortenerService.retrieveOriginalUrl(id, ipAddress, userAgent, referrer);
            if (originalUrl.isPresent()) {
                return Response.temporaryRedirect(URI.create(originalUrl.get())).build();
            } else {
                ErrorDTO errorDTO = new ErrorDTO("URL not found. The shortened URL may have expired or is invalid.", Response.Status.NOT_FOUND.getStatusCode());
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errorDTO)
                        .build();
            }
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorDTO)
                    .build();
        } catch (Exception e) {
            logger.severe("Error retrieving original URL for ID: " + id + " - " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO("Failed to retrieve original URL. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorDTO)
                    .build();
        }
    }
}
