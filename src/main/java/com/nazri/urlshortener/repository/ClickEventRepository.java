package com.nazri.urlshortener.repository;

import com.nazri.urlshortener.model.ClickEvent;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ClickEventRepository {

    private final DynamoDbTable<ClickEvent> clickEventTable;

    @Inject
    public ClickEventRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.clickEventTable = dynamoDbEnhancedClient.table("click_events", TableSchema.fromBean(ClickEvent.class));
    }

    public void save(ClickEvent clickEvent) {
        clickEventTable.putItem(clickEvent);
    }

    public List<ClickEvent> findByShortUrlId(String shortUrlId) {
        QueryConditional queryConditional = QueryConditional.partitionValue(shortUrlId);
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
