package com.nazri.urlshortener.repository;

import com.nazri.urlshortener.model.UrlEntry;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class UrlEntryRepository {

    private static final Logger log = Logger.getLogger(UrlEntryRepository.class);

    private final DynamoDbTable<UrlEntry> urlTable;
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public UrlEntryRepository(
            DynamoDbEnhancedClient dynamoDbEnhancedClient,
            DynamoDbClient dynamoDbClient,
            @ConfigProperty(name = "app.dynamodb.url-table") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        try {
            this.urlTable = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(UrlEntry.class));
            log.infof("DynamoDB URL table configured: %s", tableName);
        } catch (DynamoDbException e) {
            log.error("Error configuring DynamoDB table: " + e.getMessage(), e);
            throw new RuntimeException("DynamoDB table setup failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during table setup: " + e.getMessage(), e);
            throw new RuntimeException("Unexpected error during DynamoDB table setup", e);
        }
    }

    public void save(UrlEntry urlEntry) {
        urlTable.putItem(PutItemEnhancedRequest.builder(UrlEntry.class).item(urlEntry).build());
    }

    public Optional<UrlEntry> findById(String id) {
        UrlEntry urlEntry = urlTable.getItem(r -> r.key(k -> k.partitionValue(id)));
        return Optional.ofNullable(urlEntry);
    }

    public void update(UrlEntry urlEntry) {
        urlTable.updateItem(UpdateItemEnhancedRequest.builder(UrlEntry.class).item(urlEntry).build());
    }

    /**
     * Atomically increments the access count and updates the last-accessed timestamp
     * for the entry identified by {@code id}. Uses a low-level DynamoDB update
     * expression so concurrent clicks cannot lose increments via read-modify-write
     * races.
     *
     * @param id        partition key of the entry
     * @param timestamp epoch-seconds value to store in {@code lastAccessedDate}
     */
    public void incrementAccessCount(String id, long timestamp) {
        dynamoDbClient.updateItem(b -> b
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.fromS(id)))
                .updateExpression("SET accessCount = if_not_exists(accessCount, :z) + :inc, lastAccessedDate = :ts")
                .expressionAttributeValues(Map.of(
                        ":z", AttributeValue.fromN("0"),
                        ":inc", AttributeValue.fromN("1"),
                        ":ts", AttributeValue.fromN(Long.toString(timestamp))
                )));
    }
}
