package com.nazri.urlshortener.resource;

import com.nazri.urlshortener.dto.ErrorDTO;
import com.nazri.urlshortener.service.UrlShortenerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
     * 
     * @param id The ID of the shortened URL
     * @return A Response containing the click events data
     */
    @GET
    @Path("clicks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClickEvents(@PathParam("id") String id) {
        try {
            // Validate input
            urlShortenerService.validateUrlId(id);
            
            return Response.ok(urlShortenerService.getClickEventsForUrl(id)).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorDTO)
                    .build();
        } catch (Exception e) {
            logger.severe("Error retrieving click events for URL ID: " + id + " - " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO("Failed to retrieve click events. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorDTO)
                    .build();
        }
    }

    /**
     * Retrieves the count of unique clicks for a specific shortened URL.
     * 
     * @param id The ID of the shortened URL
     * @return A Response containing the unique click count
     */
    @GET
    @Path("unique-clicks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUniqueClicks(@PathParam("id") String id) {
        try {
            // Validate input
            urlShortenerService.validateUrlId(id);
            
            return Response.ok(String.format("{\"uniqueClicks\": %d}", urlShortenerService.getUniqueClicks(id))).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorDTO)
                    .build();
        } catch (Exception e) {
            logger.severe("Error retrieving unique clicks for URL ID: " + id + " - " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO("Failed to retrieve unique clicks. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorDTO)
                    .build();
        }
    }

    /**
     * Retrieves click statistics by country for a specific shortened URL.
     * 
     * @param id The ID of the shortened URL
     * @return A Response containing the click statistics by country
     */
    @GET
    @Path("clicks-by-country")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClicksByCountry(@PathParam("id") String id) {
        try {
            // Validate input
            urlShortenerService.validateUrlId(id);
            
            return Response.ok(urlShortenerService.getClicksByCountry(id)).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorDTO)
                    .build();
        } catch (Exception e) {
            logger.severe("Error retrieving clicks by country for URL ID: " + id + " - " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO("Failed to retrieve clicks by country. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorDTO)
                    .build();
        }
    }

    /**
     * Retrieves click statistics by referrer for a specific shortened URL.
     * 
     * @param id The ID of the shortened URL
     * @return A Response containing the click statistics by referrer
     */
    @GET
    @Path("clicks-by-referrer")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClicksByReferrer(@PathParam("id") String id) {
        try {
            // Validate input
            urlShortenerService.validateUrlId(id);
            
            return Response.ok(urlShortenerService.getClicksByReferrer(id)).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorDTO)
                    .build();
        } catch (Exception e) {
            logger.severe("Error retrieving clicks by referrer for URL ID: " + id + " - " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO("Failed to retrieve clicks by referrer. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorDTO)
                    .build();
        }
    }

    /**
     * Retrieves click statistics by user agent for a specific shortened URL.
     * 
     * @param id The ID of the shortened URL
     * @return A Response containing the click statistics by user agent
     */
    @GET
    @Path("clicks-by-user-agent")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClicksByUserAgent(@PathParam("id") String id) {
        try {
            // Validate input
            urlShortenerService.validateUrlId(id);
            
            return Response.ok(urlShortenerService.getClicksByUserAgent(id)).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorDTO)
                    .build();
        } catch (Exception e) {
            logger.severe("Error retrieving clicks by user agent for URL ID: " + id + " - " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO("Failed to retrieve clicks by user agent. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorDTO)
                    .build();
        }
    }

    /**
     * Retrieves click events within a specific time range for a shortened URL.
     * 
     * @param id The ID of the shortened URL
     * @param startTime The start time (in milliseconds) for the time range
     * @param endTime The end time (in milliseconds) for the time range
     * @return A Response containing the click events within the specified time range
     */
    @GET
    @Path("clicks-in-time-range")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClicksInTimeRange(
            @PathParam("id") String id,
            @QueryParam("startTime") Long startTime,
            @QueryParam("endTime") Long endTime) {
        try {
            // Validate input
            urlShortenerService.validateUrlId(id);
            urlShortenerService.validateTimeRange(startTime, endTime);
            
            return Response.ok(urlShortenerService.getClickEventsInTimeRange(id, startTime, endTime)).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Validation error: " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorDTO)
                    .build();
        } catch (Exception e) {
            logger.severe("Error retrieving clicks in time range for URL ID: " + id + " - " + e.getMessage());
            ErrorDTO errorDTO = new ErrorDTO("Failed to retrieve clicks in time range. Please try again later.", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorDTO)
                    .build();
        }
    }
}
