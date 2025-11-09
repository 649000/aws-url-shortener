package com.nazri.urlshortener.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
public class UrlEntry {

    private String id;
    private String originalUrl;
    private String userId;
    private Long creationDate;
    private Long lastAccessedDate;
    private Long accessCount;
    private Long expirationDate;
    private String customAlias;
    private String status;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAttribute("originalUrl")
    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("creationDate")
    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    @DynamoDbAttribute("lastAccessedDate")
    public Long getLastAccessedDate() {
        return lastAccessedDate;
    }

    public void setLastAccessedDate(Long lastAccessedDate) {
        this.lastAccessedDate = lastAccessedDate;
    }

    @DynamoDbAttribute("accessCount")
    public Long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Long accessCount) {
        this.accessCount = accessCount;
    }

    @DynamoDbAttribute("expirationDate")
    public Long getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Long expirationDate) {
        this.expirationDate = expirationDate;
    }

    @DynamoDbAttribute("customAlias")
    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UrlEntry{" +
               "id='" + id + '\'' +
               ", originalUrl='" + originalUrl + '\'' +
               ", userId='" + userId + '\'' +
               ", creationDate=" + creationDate +
               ", lastAccessedDate=" + lastAccessedDate +
               ", accessCount=" + accessCount +
               ", expirationDate=" + expirationDate +
               ", customAlias='" + customAlias + '\'' +
               ", status='" + status + '\'' +
               '}';
    }
}
