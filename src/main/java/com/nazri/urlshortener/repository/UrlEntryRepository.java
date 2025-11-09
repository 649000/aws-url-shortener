package com.nazri.urlshortener.repository;

import com.nazri.urlshortener.model.UrlEntry;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Optional;

@ApplicationScoped
public class UrlEntryRepository {

    private static final Logger log = Logger.getLogger(UrlEntryRepository.class);

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private DynamoDbTable<UrlEntry> urlTable;


    public UrlEntryRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        try {
            this.urlTable = dynamoDbEnhancedClient.table("shortened_urls", TableSchema.fromBean(UrlEntry.class));
            log.info("DynamoDB client and table configured successfully.");
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
}
