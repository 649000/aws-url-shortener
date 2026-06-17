package com.nazri.urlshortener.repository;

import com.nazri.urlshortener.model.ClickEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ClickEventRepository {

    private final DynamoDbTable<ClickEvent> clickEventTable;

    public ClickEventRepository(
            DynamoDbEnhancedClient dynamoDbEnhancedClient,
            @ConfigProperty(name = "app.dynamodb.click-event-table") String tableName) {
        this.clickEventTable = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(ClickEvent.class));
    }

    public void save(ClickEvent clickEvent) {
        clickEventTable.putItem(clickEvent);
    }

    public List<ClickEvent> findByShortUrlId(String shortUrlId) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                keyBuilder -> keyBuilder.partitionValue(shortUrlId));
        return clickEventTable.query(queryConditional).items().stream().collect(Collectors.toList());
    }

    public List<ClickEvent> findByShortUrlIdAndTimeRange(String shortUrlId, Long startTime, Long endTime) {
        QueryConditional queryConditional = QueryConditional.sortBetween(
                keyBuilder -> keyBuilder.partitionValue(shortUrlId).sortValue(startTime),
                keyBuilder -> keyBuilder.partitionValue(shortUrlId).sortValue(endTime)
        );
        return clickEventTable.query(queryConditional).items().stream().collect(Collectors.toList());
    }
}
