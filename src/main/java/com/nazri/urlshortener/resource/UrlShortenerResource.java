package com.nazri.urlshortener.resource;

import com.nazri.urlshortener.dto.ErrorDTO;
import com.nazri.urlshortener.dto.ShortenRequestDTO;
import com.nazri.urlshortener.dto.ShortenResponseDTO;
import com.nazri.urlshortener.service.UrlShortenerService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.Optional;
import java.util.logging.Logger;

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
     * @param request the request body containing the original URL and optional parameters
     * @return a 200 response with the shortened URL id, or 400/500 on failure
     */
    @POST
    @Path("shorten")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response shortenUrl(@Valid ShortenRequestDTO request) {
        try {
            String userId = (request.getUserId() != null && !request.getUserId().trim().isEmpty())
                    ? request.getUserId()
                    : "anonymous";

            String shortUrlId = urlShortenerService.shortenUrl(
                    request.getUrl(), userId, request.getExpirationDate(), request.getCustomAlias());
            return Response.ok(new ShortenResponseDTO(shortUrlId)).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode()))
                    .build();
        } catch (Exception e) {
            logger.severe("Error shortening URL: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorDTO("Failed to shorten URL. Please try again later.",
                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                    .build();
        }
    }

    /**
     * Retrieves the original URL for a given shortened URL id and redirects to it.
     *
     * @param id the shortened URL id
     * @return a 302 redirect to the original URL, or 400/404/500 on failure
     */
    @GET
    @Path("{id}")
    public Response retrieveOriginalUrl(@PathParam("id") String id) {
        try {
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

            Optional<String> originalUrl = urlShortenerService.retrieveOriginalUrl(
                    id, ipAddress, userAgent, referrer);
            if (originalUrl.isPresent()) {
                return Response.temporaryRedirect(URI.create(originalUrl.get())).build();
            }
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDTO("URL not found. The shortened URL may have expired or is invalid.",
                            Response.Status.NOT_FOUND.getStatusCode()))
                    .build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode()))
                    .build();
        } catch (Exception e) {
            logger.severe("Error retrieving original URL for ID: " + id + " - " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorDTO("Failed to retrieve original URL. Please try again later.",
                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                    .build();
        }
    }
}
