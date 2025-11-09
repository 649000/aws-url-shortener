package com.nazri.urlshortener.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
public class ClickEvent {

    private String shortUrlId;
    private Long timestamp;
    private String ipAddress;
    private String userAgent;
    private String referrer;
    private String country;
    private String city;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("shortUrlId")
    public String getShortUrlId() {
        return shortUrlId;
    }

    public void setShortUrlId(String shortUrlId) {
        this.shortUrlId = shortUrlId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("timestamp")
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDbAttribute("ipAddress")
    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @DynamoDbAttribute("userAgent")
    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @DynamoDbAttribute("referrer")
    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    @DynamoDbAttribute("country")
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @DynamoDbAttribute("city")
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "ClickEvent{" +
               "shortUrlId='" + shortUrlId + '\'' +
               ", timestamp=" + timestamp +
               ", ipAddress='" + ipAddress + '\'' +
               ", userAgent='" + userAgent + '\'' +
               ", referrer='" + referrer + '\'' +
               ", country='" + country + '\'' +
               ", city='" + city + '\'' +
               '}';
    }
}
