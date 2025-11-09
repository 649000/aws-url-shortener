package com.nazri.urlshortener.repository;

import com.nazri.urlshortener.model.UrlEntry;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class UrlEntryRepository {

    private final DynamoDbTable<UrlEntry> urlTable;

    @Inject
    public UrlEntryRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.urlTable = dynamoDbEnhancedClient.table("shortened_urls", TableSchema.fromBean(UrlEntry.class));
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
