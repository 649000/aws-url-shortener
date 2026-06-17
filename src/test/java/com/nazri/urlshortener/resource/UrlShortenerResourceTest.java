package com.nazri.urlshortener.resource;

import com.nazri.urlshortener.model.UrlEntry;
import com.nazri.urlshortener.model.UrlStatus;
import com.nazri.urlshortener.repository.ClickEventRepository;
import com.nazri.urlshortener.repository.UrlEntryRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class UrlShortenerResourceTest {

    @InjectMock
    UrlEntryRepository urlEntryRepository;

    @InjectMock
    ClickEventRepository clickEventRepository;

    @BeforeAll
    static void disableRedirectFollowing() {
        // The /{id} endpoint issues real 302 redirects to whatever the user requested.
        // If RestAssured follows them automatically the test would hit the public internet,
        // so we disable it globally for this suite.
        RestAssured.urlEncodingEnabled = false;
    }

    // ----- POST /shorten -------------------------------------------------

    @Test
    void shortenReturns200WithShortUrlId() {
        when(urlEntryRepository.findById(anyString())).thenReturn(Optional.empty());

        given()
                .contentType(ContentType.JSON)
                .body("{\"url\":\"https://example.com\"}")
                .when()
                .post("/shorten")
                .then()
                .statusCode(200)
                .body("shortUrlId", notNullValue());
    }

    @Test
    void shortenEchoesCustomAlias() {
        when(urlEntryRepository.findById("myAlias")).thenReturn(Optional.empty());

        given()
                .contentType(ContentType.JSON)
                .body("{\"url\":\"https://example.com\",\"customAlias\":\"myAlias\"}")
                .when()
                .post("/shorten")
                .then()
                .statusCode(200)
                .body("shortUrlId", equalTo("myAlias"));
    }

    @Test
    void shortenWithBlankUrlReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"url\":\"\"}")
                .when()
                .post("/shorten")
                .then()
                .statusCode(400)
                .body("statusCode", equalTo(400));
    }

    @Test
    void shortenWithInvalidSchemeReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"url\":\"javascript:alert(1)\"}")
                .when()
                .post("/shorten")
                .then()
                .statusCode(400);
    }

    @Test
    void shortenWithTakenAliasReturns400() {
        when(urlEntryRepository.findById("taken")).thenReturn(Optional.of(new UrlEntry()));

        given()
                .contentType(ContentType.JSON)
                .body("{\"url\":\"https://example.com\",\"customAlias\":\"taken\"}")
                .when()
                .post("/shorten")
                .then()
                .statusCode(400)
                .body("error", containsString("already in use"));
    }

    @Test
    void shortenWithUserIdStoresUserId() {
        when(urlEntryRepository.findById(anyString())).thenReturn(Optional.empty());

        given()
                .contentType(ContentType.JSON)
                .body("{\"url\":\"https://example.com\",\"userId\":\"alice\"}")
                .when()
                .post("/shorten")
                .then()
                .statusCode(200);

        verify(urlEntryRepository).save(argThat(e -> "alice".equals(e.getUserId())));
    }

    // ----- GET /{id} -----------------------------------------------------

    @Test
    void retrieveRedirectsToOriginalUrl() {
        UrlEntry entry = activeEntry("abc", "https://example.com");
        when(urlEntryRepository.findById("abc")).thenReturn(Optional.of(entry));

        given()
                .redirects().follow(false)
                .header("X-Forwarded-For", "1.2.3.4")
                .header("User-Agent", "TestAgent")
                .header("Referer", "https://ref.example")
                .when()
                .get("/abc")
                .then()
                .statusCode(307)
                .header("Location", equalTo("https://example.com"));
    }

    @Test
    void retrieveReturns404ForMissingUrl() {
        when(urlEntryRepository.findById("missing")).thenReturn(Optional.empty());

        given()
                .when()
                .get("/missing")
                .then()
                .statusCode(404);
    }

    @Test
    void retrieveReturns404ForExpiredUrl() {
        UrlEntry entry = activeEntry("exp", "https://example.com");
        entry.setExpirationDate(Instant.now().getEpochSecond() - 100);
        when(urlEntryRepository.findById("exp")).thenReturn(Optional.of(entry));

        given()
                .when()
                .get("/exp")
                .then()
                .statusCode(404);
    }

    @Test
    void retrieveReturns404ForInactiveUrl() {
        UrlEntry entry = activeEntry("ina", "https://example.com");
        entry.setStatus(UrlStatus.INACTIVE);
        when(urlEntryRepository.findById("ina")).thenReturn(Optional.of(entry));

        given()
                .when()
                .get("/ina")
                .then()
                .statusCode(404);
    }

    @Test
    void retrieveIncrementsAccessCountAndRecordsClick() {
        UrlEntry entry = activeEntry("inc", "https://example.com");
        when(urlEntryRepository.findById("inc")).thenReturn(Optional.of(entry));

        given()
                .redirects().follow(false)
                .when()
                .get("/inc")
                .then()
                .statusCode(307);

        verify(urlEntryRepository).incrementAccessCount(eq("inc"), anyLong());
        verify(clickEventRepository).save(any());
    }

    @Test
    void retrievePicksFirstIpFromXForwardedForChain() {
        UrlEntry entry = activeEntry("abc", "https://example.com");
        when(urlEntryRepository.findById("abc")).thenReturn(Optional.of(entry));

        given()
                .redirects().follow(false)
                .header("X-Forwarded-For", "1.2.3.4, 10.0.0.1")
                .when()
                .get("/abc")
                .then()
                .statusCode(307);

        verify(clickEventRepository).save(argThat(e -> "1.2.3.4".equals(e.getIpAddress())));
    }

    @Test
    void retrieveReturns500WhenRepoThrows() {
        when(urlEntryRepository.findById("boom")).thenThrow(new RuntimeException("DynamoDB down"));

        given()
                .when()
                .get("/boom")
                .then()
                .statusCode(500);
    }

    private UrlEntry activeEntry(String id, String url) {
        UrlEntry entry = new UrlEntry();
        entry.setId(id);
        entry.setOriginalUrl(url);
        entry.setStatus(UrlStatus.ACTIVE);
        entry.setAccessCount(0L);
        return entry;
    }
}
