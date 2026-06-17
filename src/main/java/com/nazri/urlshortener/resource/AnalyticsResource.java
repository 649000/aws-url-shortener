package com.nazri.urlshortener.resource;

import com.nazri.urlshortener.dto.ErrorDTO;
import com.nazri.urlshortener.dto.UniqueClicksResponseDTO;
import com.nazri.urlshortener.service.UrlShortenerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Resource class for handling analytics-related API endpoints.
 * Provides various analytics data for shortened URLs.
 */
@Path("/{id}/analytics")
public class AnalyticsResource {

    private static final Logger logger = Logger.getLogger(AnalyticsResource.class.getName());

    @Inject
    UrlShortenerService urlShortenerService;

    /**
     * Retrieves all click events for a specific shortened URL.
     */
    @GET
    @Path("clicks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClickEvents(@PathParam("id") String id) {
        try {
            urlShortenerService.validateUrlId(id);
            List<?> events = urlShortenerService.getClickEventsForUrl(id);
            return Response.ok(events).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error retrieving click events for URL ID: " + id + " - " + e.getMessage());
            return internalError("Failed to retrieve click events. Please try again later.");
        }
    }

    /**
     * Retrieves the count of unique clicks for a specific shortened URL.
     */
    @GET
    @Path("unique-clicks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUniqueClicks(@PathParam("id") String id) {
        try {
            urlShortenerService.validateUrlId(id);
            return Response.ok(new UniqueClicksResponseDTO(urlShortenerService.getUniqueClicks(id))).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error retrieving unique clicks for URL ID: " + id + " - " + e.getMessage());
            return internalError("Failed to retrieve unique clicks. Please try again later.");
        }
    }

    /**
     * Retrieves click statistics by country for a specific shortened URL.
     */
    @GET
    @Path("clicks-by-country")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClicksByCountry(@PathParam("id") String id) {
        try {
            urlShortenerService.validateUrlId(id);
            Map<String, Long> stats = urlShortenerService.getClicksByCountry(id);
            return Response.ok(stats).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error retrieving clicks by country for URL ID: " + id + " - " + e.getMessage());
            return internalError("Failed to retrieve clicks by country. Please try again later.");
        }
    }

    /**
     * Retrieves click statistics by referrer for a specific shortened URL.
     */
    @GET
    @Path("clicks-by-referrer")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClicksByReferrer(@PathParam("id") String id) {
        try {
            urlShortenerService.validateUrlId(id);
            Map<String, Long> stats = urlShortenerService.getClicksByReferrer(id);
            return Response.ok(stats).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error retrieving clicks by referrer for URL ID: " + id + " - " + e.getMessage());
            return internalError("Failed to retrieve clicks by referrer. Please try again later.");
        }
    }

    /**
     * Retrieves click statistics by user agent for a specific shortened URL.
     */
    @GET
    @Path("clicks-by-user-agent")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClicksByUserAgent(@PathParam("id") String id) {
        try {
            urlShortenerService.validateUrlId(id);
            Map<String, Long> stats = urlShortenerService.getClicksByUserAgent(id);
            return Response.ok(stats).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error retrieving clicks by user agent for URL ID: " + id + " - " + e.getMessage());
            return internalError("Failed to retrieve clicks by user agent. Please try again later.");
        }
    }

    /**
     * Retrieves click events within a specific time range for a shortened URL.
     */
    @GET
    @Path("clicks-in-time-range")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClicksInTimeRange(
            @PathParam("id") String id,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime) {
        try {
            urlShortenerService.validateUrlId(id);
            urlShortenerService.validateTimeRange(startTime, endTime);
            List<?> events = urlShortenerService.getClickEventsInTimeRange(id, startTime, endTime);
            return Response.ok(events).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error retrieving clicks in time range for URL ID: " + id + " - " + e.getMessage());
            return internalError("Failed to retrieve clicks in time range. Please try again later.");
        }
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDTO(message, Response.Status.BAD_REQUEST.getStatusCode()))
                .build();
    }

    private static Response internalError(String message) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorDTO(message, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .build();
    }
}
