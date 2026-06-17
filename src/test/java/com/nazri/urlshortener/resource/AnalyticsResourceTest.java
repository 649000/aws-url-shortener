 package com.nazri.urlshortener.resource;

import com.nazri.urlshortener.model.ClickEvent;
import com.nazri.urlshortener.repository.ClickEventRepository;
import com.nazri.urlshortener.repository.UrlEntryRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class AnalyticsResourceTest {

    @InjectMock
    UrlEntryRepository urlEntryRepository;

    @InjectMock
    ClickEventRepository clickEventRepository;

    @Test
    void getClicksReturnsList() {
        ClickEvent e = new ClickEvent();
        e.setShortUrlId("abc");
        e.setIpAddress("1.1.1.1");
        when(clickEventRepository.findByShortUrlId("abc")).thenReturn(List.of(e));

        given()
                .when()
                .get("/abc/analytics/clicks")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].shortUrlId", equalTo("abc"));
    }

    @Test
    void getClicksReturnsEmptyArrayWhenNoEvents() {
        when(clickEventRepository.findByShortUrlId("abc")).thenReturn(List.of());

        given()
                .when()
                .get("/abc/analytics/clicks")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    void getUniqueClicksReturnsCount() {
        ClickEvent a = new ClickEvent(); a.setIpAddress("1.1.1.1");
        ClickEvent b = new ClickEvent(); b.setIpAddress("1.1.1.1");
        ClickEvent c = new ClickEvent(); c.setIpAddress("2.2.2.2");
        when(clickEventRepository.findByShortUrlId("abc")).thenReturn(List.of(a, b, c));

        given()
                .when()
                .get("/abc/analytics/unique-clicks")
                .then()
                .statusCode(200)
                .body("uniqueClicks", equalTo(2));
    }

    @Test
    void getClicksByCountryGroupsResults() {
        ClickEvent a = new ClickEvent(); a.setCountry("SG");
        ClickEvent b = new ClickEvent(); b.setCountry("US");
        ClickEvent c = new ClickEvent(); c.setCountry("SG");
        when(clickEventRepository.findByShortUrlId("abc")).thenReturn(List.of(a, b, c));

        given()
                .when()
                .get("/abc/analytics/clicks-by-country")
                .then()
                .statusCode(200)
                .body("SG", equalTo(2))
                .body("US", equalTo(1));
    }

    @Test
    void getClicksByReferrerGroupsResults() {
        ClickEvent a = new ClickEvent(); a.setReferrer("https://a");
        ClickEvent b = new ClickEvent(); b.setReferrer("https://a");
        ClickEvent c = new ClickEvent(); c.setReferrer("https://b");
        when(clickEventRepository.findByShortUrlId("abc")).thenReturn(List.of(a, b, c));

        given()
                .when()
                .get("/abc/analytics/clicks-by-referrer")
                .then()
                .statusCode(200)
                .body("'https://a'", equalTo(2))
                .body("'https://b'", equalTo(1));
    }

    @Test
    void getClicksByUserAgentGroupsResults() {
        ClickEvent a = new ClickEvent(); a.setUserAgent("Mozilla");
        ClickEvent b = new ClickEvent(); b.setUserAgent("Chrome");
        when(clickEventRepository.findByShortUrlId("abc")).thenReturn(List.of(a, b));

        given()
                .when()
                .get("/abc/analytics/clicks-by-user-agent")
                .then()
                .statusCode(200)
                .body("Mozilla", equalTo(1))
                .body("Chrome", equalTo(1));
    }

    @Test
    void getClicksInTimeRangeDelegates() {
        ClickEvent e = new ClickEvent(); e.setShortUrlId("abc");
        when(clickEventRepository.findByShortUrlIdAndTimeRange("abc", 1L, 100L)).thenReturn(List.of(e));

        given()
                .queryParam("startTime", 1L)
                .queryParam("endTime", 100L)
                .when()
                .get("/abc/analytics/clicks-in-time-range")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    void getClicksInTimeRangeRejectsInvertedRange() {
        given()
                .queryParam("startTime", 100L)
                .queryParam("endTime", 1L)
                .when()
                .get("/abc/analytics/clicks-in-time-range")
                .then()
                .statusCode(400);
    }

    @Test
    void getClicksInTimeRangeRequiresParams() {
        given()
                .when()
                .get("/abc/analytics/clicks-in-time-range")
                .then()
                .statusCode(400);
    }

    @Test
    void getUniqueClicksReturns500WhenRepoThrows() {
        when(clickEventRepository.findByShortUrlId("abc"))
                .thenThrow(new RuntimeException("DynamoDB down"));

        given()
                .when()
                .get("/abc/analytics/unique-clicks")
                .then()
                .statusCode(500);
    }
}
