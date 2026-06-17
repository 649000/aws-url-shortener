package com.nazri.urlshortener.repository;

import com.nazri.urlshortener.model.ClickEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickEventRepositoryTest {

    @Mock
    DynamoDbEnhancedClient enhancedClient;

    @Mock
    @SuppressWarnings("unchecked")
    DynamoDbTable<ClickEvent> clickTable;

    private ClickEventRepository repository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(enhancedClient.table(eq("click-events"), any(TableSchema.class))).thenReturn(clickTable);
        repository = new ClickEventRepository(enhancedClient, "click-events");
    }

    @Test
    void saveDelegatesToEnhancedClient() {
        ClickEvent event = new ClickEvent();
        event.setShortUrlId("abc");
        event.setTimestamp(1L);
        repository.save(event);
        verify(clickTable).putItem(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByShortUrlIdQueriesAndCollects() {
        ClickEvent event = new ClickEvent();
        event.setShortUrlId("abc");
        Page<ClickEvent> page = Page.builder(ClickEvent.class).items(List.of(event)).build();
        PageIterable<ClickEvent> pageIterable = PageIterable.create(singletonIterable(page));
        when(clickTable.query(any(QueryConditional.class))).thenReturn(pageIterable);

        List<ClickEvent> result = repository.findByShortUrlId("abc");

        assertEquals(1, result.size());
        assertEquals("abc", result.get(0).getShortUrlId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByShortUrlIdReturnsEmptyWhenNoResults() {
        PageIterable<ClickEvent> empty = PageIterable.create(singletonIterable(null));
        when(clickTable.query(any(QueryConditional.class))).thenReturn(empty);

        List<ClickEvent> result = repository.findByShortUrlId("missing");
        assertEquals(0, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByShortUrlIdAndTimeRangeQueriesAndCollects() {
        ClickEvent event = new ClickEvent();
        event.setShortUrlId("abc");
        event.setTimestamp(50L);
        Page<ClickEvent> page = Page.builder(ClickEvent.class).items(List.of(event)).build();
        PageIterable<ClickEvent> pageIterable = PageIterable.create(singletonIterable(page));
        when(clickTable.query(any(QueryConditional.class))).thenReturn(pageIterable);

        List<ClickEvent> result = repository.findByShortUrlIdAndTimeRange("abc", 1L, 100L);
        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).getTimestamp());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByShortUrlIdAndTimeRangeReturnsEmpty() {
        PageIterable<ClickEvent> empty = PageIterable.create(singletonIterable(null));
        when(clickTable.query(any(QueryConditional.class))).thenReturn(empty);

        List<ClickEvent> result = repository.findByShortUrlIdAndTimeRange("abc", 1L, 100L);
        assertEquals(0, result.size());
    }

    /**
     * Returns an {@link SdkIterable} backed by a single-element iterator.
     * Used to drive {@link PageIterable#create} from tests.
     */
    private static <T> SdkIterable<T> singletonIterable(T value) {
        return () -> new Iterator<T>() {
            private boolean delivered = false;
            @Override
            public boolean hasNext() {
                return value != null && !delivered;
            }
            @Override
            public T next() {
                delivered = true;
                return value;
            }
        };
    }
}
