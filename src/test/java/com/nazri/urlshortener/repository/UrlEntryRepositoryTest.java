package com.nazri.urlshortener.repository;

import com.nazri.urlshortener.model.UrlEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlEntryRepositoryTest {

    @Mock
    DynamoDbEnhancedClient enhancedClient;

    @Mock
    DynamoDbClient dynamoDbClient;

    @Mock
    @SuppressWarnings("unchecked")
    DynamoDbTable<UrlEntry> urlTable;

    private UrlEntryRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(enhancedClient.table(eq("url-entries"), any(TableSchema.class))).thenReturn(urlTable);
        repository = new UrlEntryRepository(enhancedClient, dynamoDbClient, "url-entries");
    }

    @Test
    void saveDelegatesToEnhancedClient() {
        UrlEntry entry = new UrlEntry();
        entry.setId("abc");
        repository.save(entry);

        ArgumentCaptor<PutItemEnhancedRequest<UrlEntry>> captor =
                ArgumentCaptor.forClass(PutItemEnhancedRequest.class);
        verify(urlTable).putItem(captor.capture());
        assertEquals("abc", captor.getValue().item().getId());
    }

    @Test
    void findByIdReturnsValueWhenPresent() {
        UrlEntry stored = new UrlEntry();
        stored.setId("abc");
        when(urlTable.getItem(any(java.util.function.Consumer.class))).thenReturn(stored);

        Optional<UrlEntry> result = repository.findById("abc");

        assertTrue(result.isPresent());
        assertEquals("abc", result.get().getId());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(urlTable.getItem(any(java.util.function.Consumer.class))).thenReturn(null);

        Optional<UrlEntry> result = repository.findById("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void updateDelegatesToEnhancedClient() {
        UrlEntry entry = new UrlEntry();
        entry.setId("abc");
        repository.update(entry);

        ArgumentCaptor<UpdateItemEnhancedRequest<UrlEntry>> captor =
                ArgumentCaptor.forClass(UpdateItemEnhancedRequest.class);
        verify(urlTable).updateItem(captor.capture());
        assertEquals("abc", captor.getValue().item().getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void incrementAccessCountBuildsExpectedUpdateExpression() {
        // The repository calls dynamoDbClient.updateItem(Consumer<Builder>).
        // Capture the consumer, invoke it against a real builder, then return the response.
        ArgumentCaptor<Consumer<UpdateItemRequest.Builder>> consumerCaptor =
                ArgumentCaptor.forClass(Consumer.class);
        when(dynamoDbClient.updateItem(consumerCaptor.capture()))
                .thenReturn(UpdateItemResponse.builder().build());

        repository.incrementAccessCount("abc", 1234567890L);

        UpdateItemRequest.Builder builder = UpdateItemRequest.builder();
        consumerCaptor.getValue().accept(builder);
        UpdateItemRequest req = builder.build();

        assertEquals("url-entries", req.tableName());
        assertEquals(Map.of("id", AttributeValue.fromS("abc")), req.key());
        assertTrue(req.updateExpression().contains("accessCount"));
        assertTrue(req.updateExpression().contains("lastAccessedDate"));
        Map<String, AttributeValue> values = req.expressionAttributeValues();
        assertEquals("1", values.get(":inc").n());
        assertEquals("0", values.get(":z").n());
        assertEquals("1234567890", values.get(":ts").n());
    }

    @Test
    void constructorWrapsDynamoDbExceptionInRuntimeException() {
        @SuppressWarnings("unchecked")
        DynamoDbEnhancedClient badClient = mock(DynamoDbEnhancedClient.class);
        when(badClient.table(eq("url-entries"), any(TableSchema.class)))
                .thenThrow(DynamoDbException.builder().message("simulated failure").build());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> new UrlEntryRepository(badClient, dynamoDbClient, "url-entries"));
        assertEquals("DynamoDB table setup failed", ex.getMessage());
        assertTrue(ex.getCause() instanceof DynamoDbException);
    }
}
